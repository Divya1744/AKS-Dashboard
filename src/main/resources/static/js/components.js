/**
 * Presentation Components
 * Generates HTML fragments for the dashboard view.
 */
const Components = {
  renderSidebarList(namespaces, activeNamespace) {
    if (!namespaces || namespaces.length === 0) {
      return '<div style="padding: 1rem; color: var(--text-muted); font-size: 0.85rem;">No namespaces found.</div>';
    }

    return namespaces.map(ns => {
      const isActive = ns.name === activeNamespace;
      const activeClass = isActive ? ' active' : '';
      return `
        <div class="namespace-item${activeClass}" data-namespace="${ns.name}">
          <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
          </svg>
          <span style="font-family: var(--font-mono); font-size: 0.85rem;">${ns.name}</span>
        </div>
      `;
    }).join('');
  },

  /**
   * Renders the Cluster Overview and Node Telemetry
   */
  renderClusterView(clusterData) {
    const {
      clusterName = 'plm-rd-us',
      kubernetesVersion = 'Unknown',
      totalNodes = 0,
      readyNodes = 0,
      notReadyNodes = 0,
      totalNamespaces = 0,
      totalPods = 0,
      runningPods = 0,
      pendingPods = 0,
      failedPods = 0,
      cpu = {},
      memory = {},
      nodes = []
    } = clusterData;

    const cpuUtil = (cpu.utilization || 0).toFixed(1);
    const cpuProgress = Math.min(cpu.utilization || 0, 100);
    const memUtil = (memory.utilization || 0).toFixed(1);
    const memProgress = Math.min(memory.utilization || 0, 100);

    const nodeRowsHtml = nodes.length > 0
      ? nodes.map((node, idx) => Components.renderNodeRow(node, idx)).join('')
      : '<div class="pod-table-empty">No nodes found in this cluster.</div>';

    return `
      <!-- Cluster Header Section -->
      <section class="detail-header">
        <div class="detail-title-group">
          <h1>Cluster: ${clusterName}</h1>
          <div class="meta-pill-group">
            <span class="badge badge-info">K8s: ${kubernetesVersion}</span>
           
           
          </div>
        </div>
      </section>

      <!-- Cluster Stat Grid -->
      <section class="stat-summary-grid">
        <div class="stat-summary-card">
          <div class="stat-title">NAMESPACES</div>
          <div class="stat-number">${totalNamespaces}</div>
          <div class="stat-meta">Active isolated environments</div>
        </div>
        
        <div class="stat-summary-card">
          <div class="stat-title">POD HEALTH</div>
          <div class="stat-number">${runningPods} <span class="stat-denom">/ ${totalPods}</span></div>
          <div class="stat-meta">
            ${
              totalPods > 0 && runningPods === totalPods
                ? '<span class="text-success">All pods operational</span>'
                : `
                  <span class="text-success">${runningPods} Running</span>
                  ${pendingPods > 0 ? `&bull; <span class="text-warning">${pendingPods} Pending</span>` : ''}
                  ${failedPods > 0 ? `&bull; <span class="text-danger">${failedPods} Failed</span>` : ''}
                `.trim()
            }
          </div>
        </div>

        <div class="stat-summary-card">
          <div class="stat-title">NODE POOL HEALTH</div>
          <div class="stat-number">${readyNodes} <span class="stat-denom">/ ${totalNodes}</span></div>
          <div class="stat-meta">
            ${
              notReadyNodes === 0
                ? '<span class="text-success">All nodes operational</span>'
                : `<span class="text-success">${readyNodes} Ready</span> &bull; <span class="text-danger">${notReadyNodes} NotReady</span>`
            }
          </div>
        </div>
      </section>

      <!-- Cluster Resource Utilization Metrics -->
      <section class="metrics-grid">
        <div class="metric-card">
          <div class="metric-card-header">
            <span>CLUSTER CPU UTILIZATION</span>
            <span>${cpuUtil}%</span>
          </div>
          <div class="metric-card-value">${cpu.usage || '0m'}</div>
          <div class="metric-progress-track">
            <div class="metric-progress-fill" style="width: ${cpuProgress}%;"></div>
          </div>
          <div class="metric-breakdown">
            <span>Request: ${cpu.request || '0m'}</span>
            <span>Limit: ${cpu.limit || '0m'}</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-card-header">
            <span>CLUSTER MEMORY UTILIZATION</span>
            <span>${memUtil}%</span>
          </div>
          <div class="metric-card-value">${memory.usage || '0Gi'}</div>
          <div class="metric-progress-track">
            <div class="metric-progress-fill" style="width: ${memProgress}%;"></div>
          </div>
          <div class="metric-breakdown">
            <span>Request: ${memory.request || '0Gi'}</span>
            <span>Limit: ${memory.limit || '0Gi'}</span>
          </div>
        </div>
      </section>

      <!-- Node Inventory Breakdown -->
      <section class="workloads-section">
        <div class="workloads-header">
          <h2 class="section-title">
            <span>Cluster Nodes</span>
            <span class="badge badge-muted">${nodes.length}</span>
          </h2>
        </div>

        <div class="k8s-pod-table">
          <div class="pod-table-head node-row-grid">
            <div class="col-expander"></div>
            <div class="col-name">Node Name</div>
            <div class="col-status">Status</div>
            <div class="col-node-pods">Pods</div>
            <div class="col-numeric">CPU Usage</div>
            <div class="col-numeric">Memory Usage</div>
          </div>
          <div class="pod-table-body">
            ${nodeRowsHtml}
          </div>
        </div>
      </section>
    `;
  },

  /**
   * Renders a single compact Node row with an expandable resource chart drawer
   */
  renderNodeRow(node, index) {
    const isReady = node.status === 'Ready';
    const statusBadge = isReady ? 'badge-running' : 'badge-failed';

    const cpuUsageVal = parseFloat(node.cpuUsage) || 0;
    const cpuCapVal = parseFloat(node.cpuCapacity) || 1;
    const cpuPct = Math.min(Math.round((cpuUsageVal / cpuCapVal) * 100), 100);

    const memUsageVal = parseFloat(node.memoryUsage) || 0;
    const memCapVal = parseFloat(node.memoryCapacity) || 1;
    const memPct = Math.min(Math.round((memUsageVal / memCapVal) * 100), 100);

    const cpuBarColor = cpuPct > 85 ? 'var(--status-danger)' : (cpuPct > 70 ? 'var(--status-warning)' : 'var(--ptc-green)');
    const memBarColor = memPct > 85 ? 'var(--status-danger)' : (memPct > 70 ? 'var(--status-warning)' : 'var(--ptc-green)');

    // Pod count fallback (defaults to node.podCount if available)
    const podCountDisplay = (node.podCount !== undefined && node.podCount !== null) ? node.podCount : '—';

    return `
      <div class="pod-row-group" id="node-row-group-${index}">
        <div class="pod-summary-row node-row-grid" onclick="App.toggleNodeRow(${index})">
          <div class="col-expander">
            <svg class="chevron-icon" id="node-chevron-${index}" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div class="col-name" title="${node.name}">
            <div class="node-cell-name">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                <line x1="6" y1="6" x2="6.01" y2="6"></line>
                <line x1="6" y1="18" x2="6.01" y2="18"></line>
              </svg>
              <span>${node.name}</span>
            </div>
          </div>
          <div class="col-status">
            <span class="badge ${statusBadge}">${node.status}</span>
          </div>
          <div class="col-node-pods">
            <span class="badge badge-muted font-mono">${podCountDisplay}</span>
          </div>
          <div class="col-numeric cell-mono">${node.cpuUsage}</div>
          <div class="col-numeric cell-mono">${node.memoryUsage}</div>
        </div>

        <!-- Node Expandable Chart Drawer -->
        <div class="pod-expansion-drawer" id="node-drawer-${index}" style="display: none;">
          <div class="container-list-wrapper">
            <div class="container-list-header">
              <span>Node Allocation & Capacity Breakdown &bull; Allocated Pods: <strong>${podCountDisplay}</strong></span>
            </div>
            
            <div class="node-chart-grid">
              <!-- CPU Telemetry Chart Card -->
              <div class="node-chart-card">
                <div class="chart-header">
                  <div class="chart-title">CPU</div>
                  <div class="chart-pct">${node.cpuUsage} (${cpuPct}%)</div>
                </div>

                <div class="chart-bar-container">
                  <div class="chart-bar-track">
                    <div class="chart-bar-fill" style="width: ${cpuPct}%; background-color: ${cpuBarColor};"></div>
                  </div>
                </div>

                <div class="chart-metrics-row">
                  <div class="chart-stat">
                    <span class="chart-stat-label">Allocatable</span>
                    <span class="chart-stat-value">${node.cpuAllocatable}</span>
                  </div>
                  <div class="chart-stat">
                    <span class="chart-stat-label">Total Capacity</span>
                    <span class="chart-stat-value">${node.cpuCapacity}</span>
                  </div>
                </div>
              </div>

              <!-- Memory Telemetry Chart Card -->
              <div class="node-chart-card">
                <div class="chart-header">
                  <div class="chart-title">Memory</div>
                  <div class="chart-pct">${node.memoryUsage} (${memPct}%)</div>
                </div>

                <div class="chart-bar-container">
                  <div class="chart-bar-track">
                    <div class="chart-bar-fill" style="width: ${memPct}%; background-color: ${memBarColor};"></div>
                  </div>
                </div>

                <div class="chart-metrics-row">
                  <div class="chart-stat">
                    <span class="chart-stat-label">Allocatable</span>
                    <span class="chart-stat-value">${node.memoryAllocatable}</span>
                  </div>
                  <div class="chart-stat">
                    <span class="chart-stat-label">Total Capacity</span>
                    <span class="chart-stat-value">${node.memoryCapacity}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  },

  renderDetailView(data) {
    const name = data.name || 'Unknown';
    const podCount = data.podCount || 0;
    const cpu = data.cpu || {};
    const memory = data.memory || {};
    const pods = data.pods || [];
    const windchillUrl = data.windchillUrl;

    const cpuUtil = (cpu.utilization || 0).toFixed(1);
    const cpuProgress = Math.min(cpu.utilization || 0, 100);
    const memUtil = (memory.utilization || 0).toFixed(1);
    const memProgress = Math.min(memory.utilization || 0, 100);

    // Step 1: Conditionally render Launch Windchill button only if windchillUrl exists and is non-empty
    const windchillBtnHtml = (windchillUrl && windchillUrl.trim() !== '') ? `
      <a href="${windchillUrl}" target="_blank" rel="noopener noreferrer" class="btn btn-primary" title="Launch Windchill application">
        <span>Launch Windchill</span>
        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
          <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
          <polyline points="15 3 21 3 21 9"></polyline>
          <line x1="10" y1="14" x2="21" y2="3"></line>
        </svg>
      </a>
    ` : '';

    const podRowsHtml = pods.length > 0 
      ? pods.map((pod, idx) => Components.renderPodRow(pod, idx, name)).join('')
      : '<div class="pod-table-empty">No pods found in this namespace.</div>';

    return `
      <section class="detail-header">
        <div class="detail-title-group">
          <h1>${name}</h1>
          <div class="meta-pill-group">
            <span class="badge badge-muted">Pods: ${podCount}</span>
          </div>
        </div>
        <div class="detail-actions">
          ${windchillBtnHtml}
        </div>
      </section>

      <section class="metrics-grid">
        <div class="metric-card">
          <div class="metric-card-header">
            <span>CPU UTILIZATION</span>
            <span>${cpuUtil}%</span>
          </div>
          <div class="metric-card-value">${cpu.usage || '0m'}</div>
          <div class="metric-progress-track">
            <div class="metric-progress-fill" style="width: ${cpuProgress}%;"></div>
          </div>
          <div class="metric-breakdown">
            <span>Request: ${cpu.request || '0m'}</span>
            <span>Limit: ${cpu.limit || '0m'}</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-card-header">
            <span>MEMORY UTILIZATION</span>
            <span>${memUtil}%</span>
          </div>
          <div class="metric-card-value">${memory.usage || '0Gi'}</div>
          <div class="metric-progress-track">
            <div class="metric-progress-fill" style="width: ${memProgress}%;"></div>
          </div>
          <div class="metric-breakdown">
            <span>Request: ${memory.request || '0Gi'}</span>
            <span>Limit: ${memory.limit || '0Gi'}</span>
          </div>
        </div>
      </section>

      <section class="workloads-section">
        <div class="workloads-header">
          <h2 class="section-title">
            <span>Pod Workloads</span>
            <span class="badge badge-muted">${pods.length}</span>
          </h2>
        </div>

        <div class="k8s-pod-table">
          <div class="pod-table-head">
            <div class="col-expander"></div>
            <div class="col-name">Name</div>
            <div class="col-ready">Ready</div>
            <div class="col-status">Status</div>
            <div class="col-node">Node</div>
            <div class="col-age">Age</div>
            <div class="col-restarts">Restarts</div>
          </div>
          <div class="pod-table-body">
            ${podRowsHtml}
          </div>
        </div>
      </section>
    `;
  },

  renderPodRow(pod, index, currentNamespace) {
    const isRunning = pod.status === 'Running';
    const statusBadgeClass = isRunning ? 'badge-running' : pod.status === 'Pending' ? 'badge-pending' : 'badge-failed';
    const restarts = pod.restarts || 0;
    const restartBadgeClass = restarts > 5 
      ? 'restart-badge-critical' 
      : (restarts > 0 ? 'restart-badge-warning' : 'restart-badge-zero');
    
    // Check ready state (e.g., "1/1", "0/1")
    const readyState = pod.ready || '0/0';
    const [readyCount, totalCount] = readyState.split('/').map(v => parseInt(v, 10));
    const isFullyReady = totalCount > 0 && readyCount === totalCount;
    const readyClass = isFullyReady ? 'ready-ok' : (readyCount === 0 ? 'ready-not-ok' : 'ready-partial');

    const containers = pod.containers || [];
    // Pass namespace and pod name to each container card
    const containerCardsHtml = containers.map(c => Components.renderContainerDetail(c, pod.name, currentNamespace)).join('');

    return `
      <div class="pod-row-group" id="pod-row-group-${index}">
        <div class="pod-summary-row" onclick="App.togglePodRow(${index})">
          <div class="col-expander">
            <svg class="chevron-icon" id="chevron-${index}" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div class="col-name" title="${pod.name}">
            <span class="pod-name-text">${pod.name}</span>
          </div>
          <div class="col-ready">
            <span class="ready-badge ${readyClass}">${readyState}</span>
          </div>
          <div class="col-status">
            <span class="badge ${statusBadgeClass}">${pod.status}</span>
          </div>
          <div class="col-node" title="${pod.node || 'Unassigned'}">
            <span class="node-text">${pod.node || '—'}</span>
          </div>
          <div class="col-age">
            <span>${pod.age || '—'}</span>
          </div>
          <div class="col-restarts">
            <span class="restart-pill ${restartBadgeClass}">
              ${restarts > 0 ? `<span class="restart-alert-icon">!</span>` : ''}
              ${restarts}
            </span>
          </div>
        </div>

        <div class="pod-expansion-drawer" id="pod-drawer-${index}" style="display: none;">
          <div class="container-list-wrapper">
            <div class="container-list-header">
              <span class="containers-label">Containers (${containers.length})</span>
            </div>
            <div class="container-grid">
              ${containerCardsHtml}
            </div>
          </div>
        </div>
      </div>
    `;
  },

  renderContainerDetail(container, podName, namespace) {
    const cpu = container.cpu || {};
    const mem = container.memory || {};

    const cpuPct = Math.max(0, cpu.utilization || 0);
    const memPct = Math.max(0, memoryPctSafe(mem.utilization));
    const cpuWidth = Math.min(cpuPct, 100);
    const memWidth = Math.min(memPct, 100);

    const cpuBarColor = cpuPct > 90 ? 'var(--status-danger)' : (cpuPct > 75 ? 'var(--status-warning)' : 'var(--ptc-green)');
    const memBarColor = memPct > 90 ? 'var(--status-danger)' : (memPct > 75 ? 'var(--status-warning)' : 'var(--ptc-green)');

    // Link to logs.html in a new window/tab
    const logUrl = `logs.html?cluster=plm-rd-us&namespace=${encodeURIComponent(namespace)}&pod=${encodeURIComponent(podName)}&container=${encodeURIComponent(container.name)}`;

    return `
      <div class="container-card">
        <div class="container-card-header">
          <div class="container-card-top-row">
            <div class="container-title-line">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                <line x1="6" y1="6" x2="6.01" y2="6"></line>
                <line x1="6" y1="18" x2="6.01" y2="18"></line>
              </svg>
              <span class="container-name">${container.name}</span>
            </div>

            <a href="${logUrl}" target="_blank" rel="noopener noreferrer" class="btn-logs" title="Open container logs in terminal window">
              <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="2" fill="none">
                <polyline points="4 17 10 11 4 5"></polyline>
                <line x1="12" y1="19" x2="20" y2="19"></line>
              </svg>
              <span>Logs</span>
            </a>
          </div>

          <div class="container-image-subtle" title="${container.image || 'None'}">
            <span class="image-label">Tag:</span> ${container.image || 'None'}
          </div>
        </div>

        <div class="container-resources">
          <div class="resource-block">
            <div class="resource-block-header">
              <span class="res-type">CPU</span>
              <span class="res-usage-pct">${cpu.usage || '0m'} (${cpuPct.toFixed(1)}%)</span>
            </div>
            <div class="mini-bar-track">
              <div class="mini-bar-fill" style="width: ${cpuWidth}%; background-color: ${cpuBarColor};"></div>
            </div>
            <div class="resource-specs">
              <span>Req: <strong>${cpu.request || '0m'}</strong></span>
              <span>Limit: <strong>${cpu.limit || '0m'}</strong></span>
            </div>
          </div>

          <div class="resource-block">
            <div class="resource-block-header">
              <span class="res-type">Memory</span>
              <span class="res-usage-pct">${mem.usage || '0Gi'} (${memPct.toFixed(1)}%)</span>
            </div>
            <div class="mini-bar-track">
              <div class="mini-bar-fill" style="width: ${memWidth}%; background-color: ${memBarColor};"></div>
            </div>
            <div class="resource-specs">
              <span>Req: <strong>${mem.request || '0Gi'}</strong></span>
              <span>Limit: <strong>${mem.limit || '0Gi'}</strong></span>
            </div>
          </div>
        </div>
      </div>
    `;
  },

  renderEmptyState() {
    return `
      <div class="state-container">
        <svg class="state-icon" viewBox="0 0 24 24" width="48" height="48" stroke="currentColor" stroke-width="1.5" fill="none">
          <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
          <polyline points="2 17 12 22 22 17"></polyline>
          <polyline points="2 12 12 17 22 12"></polyline>
        </svg>
        <h3>No Selection</h3>
        <p>Choose Cluster Overview or a namespace from the sidebar to inspect real-time metrics.</p>
      </div>
    `;
  },

  renderLoadingState(message = "Loading telemetry...") {
    return `
      <div class="state-container">
        <svg class="rotate" viewBox="0 0 24 24" width="36" height="36" stroke="#5ea813" stroke-width="2" fill="none">
          <line x1="12" y1="2" x2="12" y2="6"></line>
          <line x1="12" y1="18" x2="12" y2="22"></line>
          <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
          <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
          <line x1="2" y1="12" x2="6" y2="12"></line>
          <line x1="18" y1="12" x2="22" y2="12"></line>
          <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
          <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
        </svg>
        <p>${message}</p>
      </div>
    `;
  },

  renderErrorState(errorMsg) {
    return `
      <div class="state-container">
        <div class="error-banner">
          <h4 style="font-size: 1rem; font-weight: 700; margin-bottom: 0.5rem; color: #b91c1c;">Backend Communication Error</h4>
          <p style="font-size: 0.85rem; line-height: 1.4;">${errorMsg}</p>
          <div style="margin-top: 1rem;">
            <button class="btn btn-secondary btn-sm" onclick="App.refresh()">Retry Request</button>
          </div>
        </div>
      </div>
    `;
  }
};

function memoryPctSafe(val) {
  if (typeof val === 'number' && !isNaN(val)) return val;
  return 0;
}