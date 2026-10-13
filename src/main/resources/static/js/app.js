/**
 * Application Controller
 * Handles application lifecycle, UI interactions, and state management.
 */
const App = {
  state: {
    viewMode: 'cluster', // 'cluster' or 'namespace'
    namespaces: [],
    selectedNamespace: null,
    searchQuery: '',
    isLoading: false
  },

  dom: {
    navClusterTitle: null,
    sidebarClusterLink: null,
    sidebarClusterPill: null,
    namespaceList: null,
    namespaceCount: null,
    namespaceSearch: null,
    mainContent: null,
    btnRefresh: null
  },

  init() {
    this.cacheDom();
    this.bindEvents();
    this.bootLandingPage();
  },

  cacheDom() {
    this.dom.navClusterTitle = document.getElementById('nav-cluster-title');
    this.dom.sidebarClusterLink = document.getElementById('sidebar-cluster-link');
    this.dom.sidebarClusterPill = document.getElementById('sidebar-cluster-pill');
    this.dom.namespaceList = document.getElementById('namespace-list');
    this.dom.namespaceCount = document.getElementById('namespace-count');
    this.dom.namespaceSearch = document.getElementById('namespace-search');
    this.dom.mainContent = document.getElementById('main-content');
    this.dom.btnRefresh = document.getElementById('btn-refresh');
  },

  bindEvents() {
    // Refresh button event
    if (this.dom.btnRefresh) {
      this.dom.btnRefresh.addEventListener('click', () => this.refresh());
    }

    // Clicking top brand subtitle takes user to Cluster Overview
    if (this.dom.navClusterTitle) {
      this.dom.navClusterTitle.addEventListener('click', () => this.showClusterOverview());
    }

    // Clicking cluster pill or heading in sidebar takes user to Cluster Overview
    if (this.dom.sidebarClusterPill) {
      this.dom.sidebarClusterPill.addEventListener('click', () => this.showClusterOverview());
    }
    if (this.dom.sidebarClusterLink) {
      this.dom.sidebarClusterLink.addEventListener('click', () => this.showClusterOverview());
    }

    // Namespace search filtering
    if (this.dom.namespaceSearch) {
      this.dom.namespaceSearch.addEventListener('input', (e) => {
        this.state.searchQuery = e.target.value.toLowerCase().trim();
        this.renderSidebar();
      });
    }

    // Sidebar namespace selection delegation
    if (this.dom.namespaceList) {
      this.dom.namespaceList.addEventListener('click', (e) => {
        const item = e.target.closest('.namespace-item');
        if (item) {
          const nsName = item.getAttribute('data-namespace');
          this.selectNamespace(nsName);
        }
      });
    }
  },

  async bootLandingPage() {
    // 1. Show Cluster Overview on landing immediately
    this.showClusterOverview();

    // 2. Load sidebar namespaces concurrently
    this.loadNamespacesSidebar();
  },

  async loadNamespacesSidebar() {
    try {
      const namespaces = await ApiService.getNamespaces();
      this.state.namespaces = namespaces || [];
      if (this.dom.namespaceCount) {
        this.dom.namespaceCount.textContent = this.state.namespaces.length;
      }
      this.renderSidebar();
    } catch (err) {
      console.warn("Could not load namespaces list:", err);
      if (this.dom.namespaceList) {
        this.dom.namespaceList.innerHTML = `<div style="padding: 0.75rem; font-size: 0.75rem; color: var(--status-danger);">Failed to load namespaces list</div>`;
      }
    }
  },

  async showClusterOverview() {
    this.state.viewMode = 'cluster';
    this.state.selectedNamespace = null;
    this.renderSidebar();

    this.dom.mainContent.innerHTML = Components.renderLoadingState("Loading Cluster Overview (plm-rd-us)...");

    try {
      const clusterOverview = await ApiService.getClusterOverview();
      this.dom.mainContent.innerHTML = Components.renderClusterView(clusterOverview);
    } catch (err) {
      console.error("Cluster overview error:", err);
      this.dom.mainContent.innerHTML = Components.renderErrorState(err.message);
    }
  },

  async selectNamespace(namespaceName) {
    this.state.viewMode = 'namespace';
    this.state.selectedNamespace = namespaceName;
    this.renderSidebar();

    this.dom.mainContent.innerHTML = Components.renderLoadingState(`Loading telemetry for namespace: ${namespaceName}...`);

    try {
      const details = await ApiService.getNamespaceOverview(namespaceName);
      this.dom.mainContent.innerHTML = Components.renderDetailView(details);
    } catch (err) {
      console.error(`Namespace (${namespaceName}) fetch error:`, err);
      this.dom.mainContent.innerHTML = Components.renderErrorState(err.message);
    }
  },

  renderSidebar() {
    if (!this.dom.namespaceList) return;
    const filtered = this.state.namespaces.filter(ns => 
      ns.name.toLowerCase().includes(this.state.searchQuery)
    );
    this.dom.namespaceList.innerHTML = Components.renderSidebarList(filtered, this.state.selectedNamespace);
  },

  async refresh() {
  const refreshIcon = this.dom.btnRefresh ? this.dom.btnRefresh.querySelector('svg') : null;
  if (refreshIcon) refreshIcon.classList.add('rotate');

  try {
    // Only refresh the active screen to cut redundant API calls in half
    if (this.state.viewMode === 'cluster') {
      await this.showClusterOverview();
    } else if (this.state.selectedNamespace) {
      await this.selectNamespace(this.state.selectedNamespace);
    }
  } catch (err) {
    this.dom.mainContent.innerHTML = Components.renderErrorState(err.message);
  } finally {
    if (refreshIcon) {
      setTimeout(() => refreshIcon.classList.remove('rotate'), 400);
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
  },

};

// Bootstrap App
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

