/**
 * Application Controller
 * Handles application lifecycle, dynamic cluster resolution, and kubeconfig switching.
 */
const DEFAULT_CLUSTER = 'plm-rd-us';

const App = {
  state: {
    hasClusterAccess: false,
    defaultClusterName: DEFAULT_CLUSTER,
    currentClusterName: DEFAULT_CLUSTER,
    viewMode: 'cluster',
    namespaces: [],
    selectedNamespace: null,
    searchQuery: '',
    isLoading: false
  },

  dom: {
    navDefaultCluster: null,
    navSwitchedCluster: null,
    sidebarClusterLink: null,
    sidebarClusterPill: null,
    namespaceList: null,
    namespaceCount: null,
    namespaceSearch: null,
    mainContent: null,
    btnRefresh: null,
    btnUploadKubeconfig: null,
    kubeconfigFilePicker: null
  },

  init() {
    this.cacheDom();
    this.bindEvents();
    this.bootLandingPage();
  },

  cacheDom() {
    this.dom.navDefaultCluster = document.getElementById('nav-default-cluster');
    this.dom.navSwitchedCluster = document.getElementById('nav-switched-cluster');
    this.dom.sidebarClusterLink = document.getElementById('sidebar-cluster-link');
    this.dom.sidebarClusterPill = document.getElementById('sidebar-cluster-pill');
    this.dom.namespaceList = document.getElementById('namespace-list');
    this.dom.namespaceCount = document.getElementById('namespace-count');
    this.dom.namespaceSearch = document.getElementById('namespace-search');
    this.dom.mainContent = document.getElementById('main-content');
    this.dom.btnRefresh = document.getElementById('btn-refresh');
    this.dom.btnUploadKubeconfig = document.getElementById('btn-upload-kubeconfig');
    this.dom.kubeconfigFilePicker = document.getElementById('kubeconfig-file-input');
  },

  bindEvents() {
    if (this.dom.btnRefresh) {
      this.dom.btnRefresh.addEventListener('click', () => this.refresh());
    }

    if (this.dom.btnUploadKubeconfig && this.dom.kubeconfigFilePicker) {
      this.dom.btnUploadKubeconfig.addEventListener('click', () => {
        this.dom.kubeconfigFilePicker.value = '';
        this.dom.kubeconfigFilePicker.click();
      });

      this.dom.kubeconfigFilePicker.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) this.handleKubeconfigUpload(file);
      });
    }

    // Clicking Default Cluster Pill
    // In bindEvents():
    if (this.dom.navDefaultCluster) {
      this.dom.navDefaultCluster.addEventListener('click', async () => {
        // If currently on an uploaded cluster, reset back to default
        if (this.state.currentClusterName !== this.state.defaultClusterName) {
          await this.revertToDefaultCluster();
        } else if (this.state.hasClusterAccess) {
          this.showClusterOverview();
        }
      });
    }

    // Clicking Switched Cluster Pill
    if (this.dom.navSwitchedCluster) {
      this.dom.navSwitchedCluster.addEventListener('click', (e) => {
        // If clicking the revert 'x' button
        if (e.target.closest('.btn-revert-cluster')) {
          e.stopPropagation();
          this.revertToDefaultCluster();
          return;
        }
        if (this.state.hasClusterAccess) this.showClusterOverview();
      });
    }

    if (this.dom.sidebarClusterLink) {
      this.dom.sidebarClusterLink.addEventListener('click', () => {
        if (this.state.hasClusterAccess) this.showClusterOverview();
      });
    }

    if (this.dom.namespaceSearch) {
      this.dom.namespaceSearch.addEventListener('input', (e) => {
        this.state.searchQuery = e.target.value.toLowerCase().trim();
        this.renderSidebar();
      });
    }

    if (this.dom.namespaceList) {
      this.dom.namespaceList.addEventListener('click', (e) => {
        if (!this.state.hasClusterAccess) return;
        const item = e.target.closest('.namespace-item');
        if (item) {
          const nsName = item.getAttribute('data-namespace');
          this.selectNamespace(nsName);
        }
      });
    }
  },

  updateClusterNavUI() {
    const isSwitched = this.state.currentClusterName && 
                       this.state.currentClusterName.toLowerCase() !== this.state.defaultClusterName.toLowerCase();

    if (isSwitched) {
      // Default pill remains, but inactive
      if (this.dom.navDefaultCluster) {
        this.dom.navDefaultCluster.classList.remove('active');
        this.dom.navDefaultCluster.textContent = this.state.defaultClusterName;
      }
      // Switched cluster pill displays beside it
      if (this.dom.navSwitchedCluster) {
        this.dom.navSwitchedCluster.style.display = 'inline-flex';
        this.dom.navSwitchedCluster.innerHTML = `
          <span>${this.state.currentClusterName}</span>
          <span class="btn-revert-cluster" title="Revert to default session">&times;</span>
        `;
      }
    } else {
      // Default cluster is active
      if (this.dom.navDefaultCluster) {
        this.dom.navDefaultCluster.classList.add('active');
        this.dom.navDefaultCluster.textContent = this.state.defaultClusterName;
      }
      if (this.dom.navSwitchedCluster) {
        this.dom.navSwitchedCluster.style.display = 'none';
        this.dom.navSwitchedCluster.innerHTML = '';
      }
    }
  },

  async bootLandingPage() {
    if (this.dom.btnRefresh) this.dom.btnRefresh.disabled = true;
    this.dom.mainContent.innerHTML = Components.renderLoadingState(
      "Verifying kubeconfig cluster access & permissions..."
    );

    try {
      const accessCheck = await ApiService.validateClusterAccess();

      if (!accessCheck || accessCheck.status !== 'SUCCESS') {
        this.state.hasClusterAccess = false;
        this.state.currentClusterName = '';
        this.state.namespaces = [];
        this.lockoutDashboard(accessCheck || {
          status: 'RESTRICTED',
          code: 'CLUSTER_ACCESS_DENIED',
          message: 'The configured kubeconfig does not have sufficient permissions.'
        });
        return;
      }

      const clusterName = accessCheck.clusterName;
      if (!clusterName || !clusterName.trim()) {
        this.state.hasClusterAccess = false;
        this.state.currentClusterName = '';
        this.dom.mainContent.innerHTML = Components.renderErrorState(
          'The backend validated cluster access but did not return the active cluster name.'
        );
        return;
      }

      this.state.hasClusterAccess = true;
      this.state.currentClusterName = clusterName;

      if (this.dom.navClusterTitle) {
        this.dom.navClusterTitle.textContent = clusterName;
      }
      if (this.dom.namespaceSearch) {
        this.dom.namespaceSearch.disabled = false;
        this.dom.namespaceSearch.placeholder = 'Filter namespaces...';
      }

      await this.showClusterOverview();
      await this.loadNamespacesSidebar();

    } catch (err) {
      console.error('Cluster access validation error:', err);
      this.state.hasClusterAccess = false;
      this.state.currentClusterName = '';
      this.state.namespaces = [];

      if (
        err.message &&
        (err.message.includes('403') || err.message.includes('401'))
      ) {
        this.lockoutDashboard({
          status: 'RESTRICTED',
          code: err.message.includes('401')
            ? 'AUTHENTICATION_FAILED'
            : 'CLUSTER_ACCESS_DENIED',
          message: 'The configured kubeconfig does not have sufficient permissions to monitor the cluster.'
        });
      } else {
        this.dom.mainContent.innerHTML = Components.renderErrorState(
          err.message || 'An unexpected error occurred while loading the cluster.'
        );
      }
    } finally {
      // Re-enable refresh button once boot finishes
      if (this.dom.btnRefresh) this.dom.btnRefresh.disabled = false;
    }
  },

  async handleKubeconfigUpload(file) {
    this.dom.mainContent.innerHTML = Components.renderLoadingState(
      `Validating and switching to uploaded kubeconfig (${file.name})...`
    );

    try {
      const response = await ApiService.uploadKubeconfig(file);
      if (response && response.clusterName) {
        this.state.currentClusterName = response.clusterName;
      }
      this.updateClusterNavUI();
      await this.bootLandingPage();
    } catch (err) {
      console.error("Kubeconfig upload failed:", err);
      const data = err.data || {};
      this.lockoutDashboard({
        status: data.status || 'ERROR',
        code: data.code || 'UPLOAD_FAILED',
        message: err.message || 'Failed to switch kubeconfig.'
      });
    }
  },

  async revertToDefaultCluster() {
    this.dom.mainContent.innerHTML = Components.renderLoadingState("Restoring default cluster session (plm-rd-us)...");

    try {
      await ApiService.resetSession();
      // Reset state and reload so the new session initializes with default plm-rd-us
      this.state.currentClusterName = this.state.defaultClusterName;
      window.location.reload();
    } catch (err) {
      console.error("Failed to reset session:", err);
      this.dom.mainContent.innerHTML = Components.renderErrorState(
        "Could not restore default cluster session: " + err.message
      );
    }
  },

  // ... keep showClusterOverview, selectNamespace, loadNamespacesSidebar, etc.

  async showClusterOverview() {
    if (!this.state.hasClusterAccess || !this.state.currentClusterName) return;
    this.state.viewMode = 'cluster';
    this.state.selectedNamespace = null;

    this.dom.mainContent.innerHTML = Components.renderLoadingState("Loading Cluster Overview...");

    try {
      const clusterOverview = await ApiService.getClusterOverview(this.state.currentClusterName);
      if (clusterOverview && clusterOverview.clusterName) {
        this.state.currentClusterName = clusterOverview.clusterName;
        if (this.dom.navClusterTitle) {
          this.dom.navClusterTitle.textContent = clusterOverview.clusterName;
        }
      }
      this.dom.mainContent.innerHTML = Components.renderClusterView(clusterOverview);
      this.renderSidebar();
    } catch (err) {
      console.error("Cluster overview error:", err);
      this.dom.mainContent.innerHTML = Components.renderErrorState(err.message);
    }
  },

  

  lockoutDashboard(accessCheck) {
    this.dom.mainContent.innerHTML = Components.renderAccessDeniedState(accessCheck);

    if (this.dom.namespaceList) {
      this.dom.namespaceList.innerHTML = `<div style="padding: 1rem; color: var(--text-muted); font-size: 0.8rem; text-align: center;">Cluster access restricted</div>`;
    }
    if (this.dom.namespaceCount) this.dom.namespaceCount.textContent = '0';
    if (this.dom.namespaceSearch) {
      this.dom.namespaceSearch.disabled = true;
      this.dom.namespaceSearch.placeholder = 'Access restricted';
    }
  },

  async loadNamespacesSidebar() {
    if (!this.state.hasClusterAccess || !this.state.currentClusterName) return;
    try {
      const namespaces = await ApiService.getNamespaces(this.state.currentClusterName);
      this.state.namespaces = namespaces || [];
      if (this.dom.namespaceCount) {
        this.dom.namespaceCount.textContent = this.state.namespaces.length;
      }
      this.renderSidebar();
    } catch (err) {
      console.warn("Could not load namespaces list:", err);
    }
  },

  async selectNamespace(namespaceName) {
    if (!this.state.hasClusterAccess || !this.state.currentClusterName) return;
    this.state.viewMode = 'namespace';
    this.state.selectedNamespace = namespaceName;
    this.renderSidebar();

    this.dom.mainContent.innerHTML = Components.renderLoadingState(`Loading telemetry for namespace: ${namespaceName}...`);

    try {
      const details = await ApiService.getNamespaceOverview(this.state.currentClusterName, namespaceName);
      this.dom.mainContent.innerHTML = Components.renderDetailView(details, this.state.currentClusterName);
    } catch (err) {
      console.error(`Namespace (${namespaceName}) fetch error:`, err);
      this.dom.mainContent.innerHTML = Components.renderErrorState(err.message);
    }
  },

  renderSidebar() {
    if (!this.dom.namespaceList || !this.state.hasClusterAccess) return;
    const filtered = this.state.namespaces.filter(ns => 
      ns.name.toLowerCase().includes(this.state.searchQuery)
    );
    this.dom.namespaceList.innerHTML = Components.renderSidebarList(filtered, this.state.selectedNamespace);
  },

  async refresh() {
    // 1. Guard against overlapping clicks if already loading
    if (this.state.isLoading || (this.dom.btnRefresh && this.dom.btnRefresh.disabled)) {
      return;
    }

    const refreshBtn = this.dom.btnRefresh;
    const refreshIcon = refreshBtn ? refreshBtn.querySelector('svg') : null;

    // 2. Disable button & start spin animation
    this.state.isLoading = true;
    if (refreshBtn) {
      refreshBtn.disabled = true;
    }
    if (refreshIcon) {
      refreshIcon.classList.add('rotate');
    }

    try {
      if (!this.state.hasClusterAccess) {
        await this.bootLandingPage();
        return;
      }

      await this.loadNamespacesSidebar();

      if (this.state.viewMode === 'cluster') {
        await this.showClusterOverview();
      } else if (this.state.selectedNamespace) {
        await this.selectNamespace(this.state.selectedNamespace);
      }
    } catch (err) {
      console.error("Refresh failed:", err);
      this.dom.mainContent.innerHTML = Components.renderErrorState(err.message);
    } finally {
      // 3. Re-enable button & remove spin animation
      this.state.isLoading = false;
      if (refreshIcon) {
        refreshIcon.classList.remove('rotate');
      }
      if (refreshBtn) {
        refreshBtn.disabled = false;
      }
    }
  },

  togglePodRow(index) {
    const drawer = document.getElementById(`pod-drawer-${index}`);
    const rowGroup = document.getElementById(`pod-row-group-${index}`);
    if (drawer && rowGroup) {
      const isExpanded = drawer.style.display !== 'none';
      drawer.style.display = isExpanded ? 'none' : 'block';
      rowGroup.classList.toggle('open', !isExpanded);
    }
  },

  toggleNodeRow(index) {
    const drawer = document.getElementById(`node-drawer-${index}`);
    const rowGroup = document.getElementById(`node-row-group-${index}`);
    if (drawer && rowGroup) {
      const isExpanded = drawer.style.display !== 'none';
      drawer.style.display = isExpanded ? 'none' : 'block';
      rowGroup.classList.toggle('open', !isExpanded);
    }
  }
};

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});