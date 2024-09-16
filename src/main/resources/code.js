class IbkrClass extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        this.orders = [];
    }

    setContext(widget) {
        this.fetchInfo(widget);
    }

    render() {
        var positions = this.buildPositions();
        const sort = this.buildSort();
        const body = positions ?
            `<div class="stock-orders">${positions}</div><div>${sort}</div>`
            : `<div class="no-positions">No positions</div>`;
        this.content.innerHTML = body;
        this.addEventListeners();
    }

    buildPositions() {
        if (this.sort === 'POS') {
            this.info.tickers.sort((a, b) => this.sortDouble(a, b, 'position'));
        } else if (this.sort === 'P&L') {
            this.info.tickers.sort((a, b) => this.sortDouble(a, b, 'unrealizedPnl'));
        } else if (this.sort === 'O(B)') {
            this.info.tickers.sort((a, b) => this.sortOrders(a, b, 'Buy'));
        } else if (this.sort === 'O(S)') {
            this.info.tickers.sort((a, b) => this.sortOrders(a, b, 'Sell'));
        }
        return this.info.tickers.map(pos => this.buildPosition(pos)).join('');
    }

    sortDouble(a, b, key) {
        return this.asc ? a[key] - b[key] : b[key] - a[key];
    }

    sortOrders(a, b, key) {
        const l1 = (this.asc ? a : b).orders?.filter(o => o.side === key)?.length || 0;
        const l2 = (this.asc ? b : a).orders?.filter(o => o.side === key)?.length || 0;
        if (l1 > 0 && l2 === 0) return -1;
        if (l1 === 0 && l2 > 0) return 1;
        return l2 - l1;
    }

    buildSort() {
        const sortItems = ['P&L', 'POS', 'O(B)', 'O(S)'];
        return `<table class="chart-button-table">
                    <tbody>
                    <tr>
                    ${sortItems.map(s => `<td data-sort="${s}"
                        class="${this.buildSortClass(s)}">${s}
                    </td>`)}
                    </tr>
                    </tbody>
                </table>`;
    }

    buildSortClass(s) {
        if (this.sort === s) {
            return 'active ' + (this.asc ? 'asc' : 'desc');
        }
        return '';
    }

    addEventListeners() {
        const sortButtons = this.content.querySelectorAll('[data-sort]');
        sortButtons.forEach(button => {
            button.addEventListener('click', (event) => {
                this.sort = event.target.dataset.sort;
                this.asc = !this.asc;
                this.render();
            });
        });
    }

    buildPosition(pos) {
        const clazz = pos.position == 0 ? ' no-positions' : '';
        return `<div class="stock-item${clazz}">
      <div class="item">${pos.ticker}:${pos.position}</div>     
      <div class="item right ${pos.unrealizedPnl > 0 ? 'positive' : 'negative'}">
        P&L: ${pos.unrealizedPnl.toFixed(0)}
      </div>     
      <div class="item ${pos.avgPrice < pos.mktPrice ? 'positive' : 'negative'}">
        ${pos.mktPrice.toFixed(1)}/${pos.avgPrice.toFixed(1)}
      </div> 
      <div class="item right">
       ${pos.mktValue.toFixed(1)}${pos.currency}
      </div>
      <div class="item"> 
       
      </div> 
      <div class="item right item-orders">
       <div>${this.buildOrders(pos.orders)}</div>
      </div>   
      <div class="item">
       
      </div>  
    </div>`;
    }

    buildOrders(orders) {
        return `<table>${orders?.map(o =>
            `<tr class="order ${o.side === 'Sell' ? 'sell' : 'buy'}">
               <td>${o.side}</td>
               <td>${o.size}</td>
               <td>${o.type}</td>
               <td>${o.price}</td></tr>`).join('') || ''}</table>`;
    }

    fetchInfo(widget) {
        widget.callService('getWidgetInfo').subscribe(value => {
            this.info = value;
            this.sort = widget.sort || 'P&L';
            this.render();
        });
    }
}

customElements.define("ibkr-widget", IbkrClass);