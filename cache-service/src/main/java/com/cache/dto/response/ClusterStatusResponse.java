package com.cache.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the overall cluster status endpoint.
 *
 * <p>Provides a high-level summary of cluster health alongside the full list
 * of registered nodes. This is the primary payload for the Phase 11 dashboard.</p>
 *
 * <p>WHY include {@code upCount}/{@code downCount} separately from the node list?</p>
 * Dashboard consumers often need aggregate health at a glance — rendered as a
 * status badge — before they need to enumerate individual nodes.
 * Pre-computing these counts server-side avoids N iterations in every client.
 */
@Schema(description = "Overall cluster status snapshot including all registered nodes")
public record ClusterStatusResponse(

        @Schema(description = "Total number of nodes registered in the cluster")
        int totalNodes,

        @Schema(description = "Number of nodes currently in UP status")
        int upCount,

        @Schema(description = "Number of nodes in STARTING status")
        int startingCount,

        @Schema(description = "Number of nodes in SUSPECT status")
        int suspectCount,

        @Schema(description = "Number of nodes in DOWN status")
        int downCount,

        @Schema(description = "Whether the cluster is considered healthy (all nodes UP)")
        boolean clusterHealthy,

        @Schema(description = "Full list of all registered nodes with their metadata")
        List<NodeInfoResponse> nodes

) {}
