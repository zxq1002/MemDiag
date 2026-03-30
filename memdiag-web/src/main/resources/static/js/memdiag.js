// MemDiag Web UI - Common JavaScript Utilities

// Global utilities
window.MemDiag = {
    formatBytes: function(bytes) {
        if (bytes === 0 || bytes === null || bytes === undefined) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    formatNumber: function(num) {
        if (num === null || num === undefined) return '0';
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    },

    formatMs: function(ms) {
        if (ms === null || ms === undefined) return '0 ms';
        if (ms < 1) return (ms * 1000).toFixed(1) + ' µs';
        if (ms < 1000) return ms.toFixed(2) + ' ms';
        return (ms / 1000).toFixed(2) + ' s';
    },

    escapeHtml: function(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },

    formatDate: function(dateStr) {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleString();
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    // Add any global initialization here
    console.log('MemDiag Web UI initialized');
});
