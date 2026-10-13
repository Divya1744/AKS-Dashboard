/**
 * API Service
 * Handles communication with Spring Boot backend endpoints.
 */
/** const API_BASE = 'http://localhost:8080/api'; **/

const ApiService = {
  /**
   * Fetches Cluster-wide overview telemetry and node specs
   * Target: GET /api/clusters/plm-rd-us
   */
  async getClusterOverview() {
    const response = await fetch(`/api/clusters/plm-rd-us`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch cluster overview (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  },

  /**
   * Fetches all namespaces for cluster plm-rd-us
   * Target: GET /api/clusters/plm-rd-us/namespaces
   */
  async getNamespaces() {
    const response = await fetch(`/api/clusters/plm-rd-us/namespaces`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch namespaces (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  },

  /**
   * Fetches detailed telemetry for a selected namespace
   * Target: GET /api/clusters/plm-rd-us/namespaces/{namespace}
   */
  async getNamespaceOverview(namespaceName) {
    const response = await fetch(`/api/clusters/plm-rd-us/namespaces/${encodeURIComponent(namespaceName)}`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch overview for '${namespaceName}' (HTTP ${response.status}: ${response.statusText})`);
    }

    return await response.json();
  }
};