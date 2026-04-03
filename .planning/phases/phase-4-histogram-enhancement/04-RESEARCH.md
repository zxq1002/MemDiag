# Phase 4: Histogram View Enhancement - Research

**Researched:** 2026-04-02
**Domain:** Frontend (Vue 3, PrimeVue 4, ECharts 5)
**Confidence:** HIGH

## Summary

This phase focuses on improving the "Heap Histogram" view in the MemDiag UI. The current implementation provides a basic table and chart but lacks advanced analysis tools like data export, complex filtering, and intuitive visual cues for memory distribution. 

We will leverage PrimeVue 4's powerful `DataTable` features to implement CSV/JSON export and advanced filtering. We will also introduce "relative size bars" (data bars) within the table to allow users to quickly identify memory hotspots. To keep the codebase maintainable, we will componentize the histogram view and move the business logic into a dedicated composable.

**Primary recommendation:** Use PrimeVue's built-in `exportCSV` for CSV export and a custom utility for JSON export. Implement client-side advanced filtering within the `DataTable` for real-time interactivity.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue | ^3.4.0 | UI Framework | Project standard |
| PrimeVue | ^4.0.0 | Component Library | Provides high-quality DataTable and form components |
| ECharts | ^5.4.0 | Data Visualization | Used for the top-10 memory distribution chart |
| Pinia | ^2.1.0 | State Management | Used for shared connection state |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|--------------|
| lucide-vue-next | ^0.300.0 | Icons | Consistent iconography |
| @vueuse/core | ^10.7.0 | Utilities | Helper functions for reactivity and browser APIs |

**Installation:**
```bash
# No new packages required. Existing stack covers all needs.
npm install
```

## Architecture Patterns

### Recommended Project Structure
```
memdiag-ui/src/
├── components/
│   └── histogram/
│       ├── HistogramSummary.vue    # Total objects/bytes cards
│       ├── HistogramChart.vue      # ECharts visualization
│       └── HistogramTable.vue      # Main DataTable with export/filters
├── composables/
│   └── useHistogram.js             # Logic for fetching and processing data
└── views/
    └── Histogram.vue               # Main view (orchestrator)
```

### Pattern 1: Composable for Data Management
**What:** Encapsulate API calls, state (histogram data, loading), and formatting logic.
**When to use:** When a view has complex data fetching and processing logic.
**Example:**
```javascript
// src/composables/useHistogram.js
export function useHistogram(connectionId) {
  const histogram = ref(null);
  const isLoading = ref(false);
  
  const load = async (limit = 20) => {
    isLoading.value = true;
    try {
      const response = await axios.get(`/api/v1/histogram/${connectionId.value}`, { params: { limit } });
      histogram.value = response.data.data;
    } finally {
      isLoading.value = false;
    }
  };

  const classStats = computed(() => histogram.value?.classes || []);
  
  return { histogram, classStats, isLoading, load };
}
```

### Pattern 2: Inline Data Bars in DataTable
**What:** Use custom templates in PrimeVue `Column` to render relative size bars.
**Why:** Provides immediate visual context for the magnitude of values.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| CSV Export | Custom CSV generator | `dt.value.exportCSV()` | PrimeVue's built-in support handles encoding and browser compatibility. |
| Table Filtering | Custom filter logic | `DataTable` filters | Supports multi-mode, menu-based, and custom matchers natively. |
| Charting | Custom SVG/Canvas | ECharts | Industry standard, already in use, highly performant. |

## Common Pitfalls

### Pitfall 1: Large Dataset Performance
**What goes wrong:** Rendering 1000+ rows with custom templates and relative bars can cause UI lag.
**How to avoid:** Use PrimeVue's `paginator` (already in use) or `virtualScroller` if the user requests very large limits (>1000).
**Warning signs:** Scrolling becomes jittery or initial render takes >1s.

### Pitfall 2: ECharts Memory Leaks
**What goes wrong:** ECharts instances are not disposed of when switching views or refreshing data.
**How to avoid:** Always call `chart.dispose()` in `onBeforeUnmount` and properly handle `resize` events.

### Pitfall 3: Filter Precision
**What goes wrong:** Filtering for "min size" using bytes when the display shows KB/MB/GB.
**How to avoid:** Use a `InputNumber` with consistent units (e.g., always KB) or allow the user to select the unit in the filter menu.

## Code Examples

### CSV/JSON Export Implementation
```javascript
// CSV (PrimeVue)
const dt = ref();
const exportCSV = () => dt.value.exportCSV();

// JSON (Custom)
const exportJSON = (data) => {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `histogram-${Date.now()}.json`;
  link.click();
  URL.revokeObjectURL(url);
};
```

### Relative Size Bar Template
```vue
<Column field="shallowBytes" header="Shallow Size" sortable>
  <template #body="{ data }">
    <div class="flex items-center gap-2">
      <div class="flex-1 h-1.5 bg-slate-100 rounded-full overflow-hidden">
        <div 
          class="h-full bg-indigo-500 rounded-full" 
          :style="{ width: (data.shallowBytes / maxShallowBytes) * 100 + '%' }"
        ></div>
      </div>
      <span class="font-mono text-xs">{{ formatBytes(data.shallowBytes) }}</span>
    </div>
  </template>
</Column>
```

## Open Questions

1. **Filtering Scope:** Should we support server-side filtering (passing min count/size to the API)?
   - *What we know:* The current API does not support this.
   - *What's unclear:* If users will frequently need to filter through tens of thousands of classes.
   - *Recommendation:* Start with client-side filtering on a larger limit (e.g., fetch top 500-1000 classes). If performance issues arise, extend the backend API.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Development | ✓ | 20.x | — |
| npm | Dependency Mgmt | ✓ | 10.x | — |
| PrimeVue 4 | UI Components | ✓ | 4.0.0 | — |
| ECharts 5 | Visualization | ✓ | 5.4.0 | — |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest |
| Config file | `memdiag-ui/vitest.config.js` |
| Quick run command | `npm run test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| R-UI-002.1 | CSV Export | Component | `vitest run HistogramTable.test.js` | ❌ Wave 0 |
| R-UI-002.2 | JSON Export | Unit | `vitest run export.test.js` | ❌ Wave 0 |
| R-UI-002.3 | Advanced Filter | Component | `vitest run HistogramTable.test.js` | ❌ Wave 0 |
| R-UI-002.4 | Visual Bars | Component | `vitest run HistogramTable.test.js` | ❌ Wave 0 |

### Wave 0 Gaps
- [ ] `memdiag-ui/src/components/histogram/__tests__/HistogramTable.test.js` — covers export and filtering logic.
- [ ] `memdiag-ui/src/utils/__tests__/exportUtils.test.js` — covers JSON export logic.

## Sources

### Primary (HIGH confidence)
- [PrimeVue 4 Documentation](https://primevue.org/datatable/) - Checked DataTable export and filtering.
- [ECharts Documentation](https://echarts.apache.org/en/option.html) - Checked instance management and data mapping.
- `memdiag-web` Source Code - Verified API response structure for histograms.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Core libraries are already in use.
- Architecture: HIGH - Composable and componentization patterns are standard Vue 3 practices.
- Pitfalls: HIGH - Based on experience with PrimeVue and ECharts.

**Research date:** 2026-04-02
**Valid until:** 2026-05-02
