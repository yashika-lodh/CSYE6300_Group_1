/**
 * Reusable client-side pagination helper shared by every dashboard table
 * whose row count grows with tracked SKUs or audit history.
 */
window.Paginator = {
  slice(items, page, pageSize) {
    const totalItems = items.length;
    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
    const currentPage = Math.min(Math.max(1, page), totalPages);
    const start = (currentPage - 1) * pageSize;
    return {
      pageItems: items.slice(start, start + pageSize),
      currentPage,
      totalPages,
      totalItems
    };
  },

  renderControls(state) {
    if (state.totalPages <= 1) return '';
    return `
      <div class="pagination-bar">
        <button class="btn btn-secondary btn-sm" data-page-action="prev" ${state.currentPage <= 1 ? 'disabled' : ''}>&larr; Prev</button>
        <span class="pagination-status">Page ${state.currentPage} of ${state.totalPages} (${state.totalItems} total)</span>
        <button class="btn btn-secondary btn-sm" data-page-action="next" ${state.currentPage >= state.totalPages ? 'disabled' : ''}>Next &rarr;</button>
      </div>
    `;
  },

  // Wires Prev/Next clicks to mutate `module[pageProp]` and re-render via `module.refresh()`.
  attach(containerEl, module, pageProp = 'currentPage') {
    if (!containerEl) return;
    containerEl.querySelectorAll('[data-page-action]').forEach(btn => {
      btn.addEventListener('click', () => {
        module[pageProp] += btn.getAttribute('data-page-action') === 'prev' ? -1 : 1;
        module.refresh();
      });
    });
  }
};
