/**
 * API Service
 * Communicates strictly with existing DashboardController endpoints.
 */
const ApiService = {
  /**
   * Target: GET /api/clusters/access
   */
  async validateClusterAccess() {
    const response = await fetch(`/api/clusters/access`, {
      method: 'GET',
      cache: 'no-store',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      try {
        const errorJson = await response.json();
        if (errorJson && errorJson.status) return errorJson;
      } catch (_) {}

      if (response.status === 401 || response.status === 403) {
        return {
          status: 'RESTRICTED',
          code: response.status === 401 ? 'AUTHENTICATION_FAILED' : 'CLUSTER_ACCESS_DENIED',
          message: 'The configured kubeconfig does not have sufficient permissions to monitor the cluster.'
        };
      }
      throw new Error(`Failed to validate cluster access (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  },

  /**
   * Target: GET /api/kubeconfig/active-context
   */
  async getActiveContext() {
    const response = await fetch(`/api/kubeconfig/active-context`, {
      method: 'GET',
      cache: 'no-store',
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) throw new Error('Failed to fetch active context');
    return await response.json();
  },

  /**
   * Target: POST /api/kubeconfig/upload
   */
  async uploadKubeconfig(file) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`/api/kubeconfig/upload`, {
      method: 'POST',
      body: formData
    });

    const result = await response.json().catch(() => null);
    if (!response.ok) {
      const errorMsg = result?.message || `Upload failed with HTTP ${response.status}`;
      const err = new Error(errorMsg);
      err.data = result;
      throw err;
    }
    return result;
  },

  /**
   * Target: GET /api/clusters/{clusterName}
   */
  async getClusterOverview(clusterName) {
    if (!clusterName) throw new Error('clusterName is required');
    const response = await fetch(`/api/clusters/${encodeURIComponent(clusterName)}`, {
      method: 'GET',
      cache: 'no-store',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch cluster overview (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  },

  /**
   * Target: GET /api/clusters/{clusterName}/namespaces
   */
  async getNamespaces(clusterName) {
    const response = await fetch(`/api/clusters/${encodeURIComponent(clusterName)}/namespaces`, {
      method: 'GET',
      cache: 'no-store',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch namespaces (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  },

  /**
   * Target: GET /api/clusters/{clusterName}/namespaces/{namespace}
   */
  async getNamespaceOverview(clusterName, namespaceName) {
    const response = await fetch(`/api/clusters/${encodeURIComponent(clusterName)}/namespaces/${encodeURIComponent(namespaceName)}`, {
      method: 'GET',
      cache: 'no-store',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch overview for '${namespaceName}' (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  },

  /**
   * Resets the server session to restore the default cluster
   * Target: POST /api/kubeconfig/reset
   */
  async resetSession() {
    const response = await fetch(`/api/kubeconfig/reset`, {
      method: 'POST',
      cache: 'no-store',
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to reset cluster session (HTTP ${response.status})`);
    }

    return true;
  },
};