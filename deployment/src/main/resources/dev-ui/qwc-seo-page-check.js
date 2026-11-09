import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/progress-bar';

/**
 * Check the SEO head of one page or of every sitemap page.
 */
export class QwcSeoPageCheck extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 16px;
            padding: 10px;
        }
        .bar {
            display: flex;
            align-items: baseline;
            gap: 10px;
        }
        .bar vaadin-text-field {
            min-width: 300px;
        }
        .error, .warning {
            color: var(--lumo-error-text-color);
        }
        .ok {
            color: var(--lumo-success-text-color);
        }
        .snippet, .card {
            border: 1px solid var(--lumo-contrast-20pct);
            border-radius: var(--lumo-border-radius-m);
            padding: 10px;
            max-width: 600px;
        }
        .snippet-title {
            font-size: 1.2em;
            color: var(--lumo-primary-text-color);
        }
        .snippet-url, .muted {
            color: var(--lumo-contrast-60pct);
            font-size: 0.9em;
            overflow-wrap: anywhere;
        }
        .card-title {
            font-weight: bold;
        }
        pre {
            white-space: pre-wrap;
            overflow-wrap: anywhere;
            background: var(--lumo-contrast-5pct);
            padding: 10px;
        }
    `;

    static properties = {
        _path: { state: true },
        _busy: { state: true },
        _page: { state: true },
        _sitemap: { state: true }
    };

    constructor() {
        super();
        this._path = '/';
        this._busy = false;
        this._page = null;
        this._sitemap = null;
    }

    render() {
        return html`
            <div class="bar">
                <vaadin-text-field label="Path" .value="${this._path}"
                    @value-changed="${e => this._path = e.detail.value}"
                    @keydown="${e => { if (e.key === 'Enter') this._checkPage(); }}"></vaadin-text-field>
                <vaadin-button theme="primary" ?disabled="${this._busy}" @click="${this._checkPage}">Check</vaadin-button>
                <vaadin-button ?disabled="${this._busy}" @click="${this._checkSitemap}">Check sitemap</vaadin-button>
            </div>
            ${this._busy ? html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>` : html``}
            ${this._renderPage()}
            ${this._renderSitemap()}
        `;
    }

    _checkPage() {
        this._busy = true;
        this._sitemap = null;
        this.jsonRpc.checkPage({ path: this._path }).then(response => {
            this._page = response.result;
        }).catch(error => {
            this._page = { error: String(error) };
        }).finally(() => {
            this._busy = false;
        });
    }

    _checkSitemap() {
        this._busy = true;
        this._page = null;
        this.jsonRpc.checkSitemap().then(response => {
            this._sitemap = response.result;
        }).catch(error => {
            this._sitemap = { error: String(error) };
        }).finally(() => {
            this._busy = false;
        });
    }

    _renderPage() {
        const p = this._page;
        if (!p) {
            return html``;
        }
        if (p.error) {
            return html`<div class="error">${p.error}</div>`;
        }
        return html`
            <div class="muted">HTTP status ${p.status}</div>
            ${this._renderWarnings(p.warnings)}
            ${p.snippet ? html`
                <section>
                    <h3>Search snippet</h3>
                    <div class="snippet">
                        <div class="snippet-url">${p.snippet.url}</div>
                        <div class="snippet-title">${p.snippet.title}</div>
                        <div>${p.snippet.description}</div>
                    </div>
                </section>` : html``}
            ${p.card ? html`
                <section>
                    <h3>Social card</h3>
                    <div class="card">
                        <div class="muted">Image: ${p.card.image || 'none'}</div>
                        <div class="muted">${p.card.siteName}</div>
                        <div class="card-title">${p.card.title}</div>
                        <div>${p.card.description}</div>
                    </div>
                </section>` : html``}
            ${p.head ? html`
                <section>
                    <h3>Head tags</h3>
                    <pre>${p.head}</pre>
                </section>` : html``}
        `;
    }

    _renderSitemap() {
        const s = this._sitemap;
        if (!s) {
            return html``;
        }
        if (s.error) {
            return html`<div class="error">${s.error}</div>`;
        }
        const entries = s.warnings || [];
        return html`
            <div class="muted">Checked ${s.checked} pages. Skipped ${s.skipped} pages.</div>
            ${entries.length === 0 ? html`<div class="ok">No warnings.</div>` : html`
                <ul>
                    ${entries.map(entry => entry.url !== undefined ? html`
                        <li>
                            ${entry.url}${entry.status !== undefined ? html` <span class="muted">(${entry.status})</span>` : html``}
                            ${this._renderWarnings(entry.warnings)}
                        </li>` : html`<li class="warning">${entry.message} [${entry.code}]</li>`)}
                </ul>`}
        `;
    }

    _renderWarnings(warnings) {
        const list = warnings || [];
        if (list.length === 0) {
            return html`<div class="ok">No warnings.</div>`;
        }
        return html`
            <ul>
                ${list.map(w => html`<li class="warning">${w.message} [${w.code}]</li>`)}
            </ul>
        `;
    }
}
customElements.define('qwc-seo-page-check', QwcSeoPageCheck);
