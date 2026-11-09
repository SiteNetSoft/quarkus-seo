import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { outputs } from 'build-time-data';
import '@vaadin/progress-bar';

/**
 * Show the SEO site settings, the outputs and the security.txt warnings.
 */
export class QwcSeoOverview extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 20px;
            padding: 10px;
        }
        table {
            border-collapse: collapse;
        }
        th, td {
            text-align: left;
            padding: 4px 16px 4px 0;
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }
        .off {
            color: var(--lumo-contrast-50pct);
        }
        .warning {
            color: var(--lumo-error-text-color);
        }
        .error {
            color: var(--lumo-error-text-color);
        }
    `;

    static properties = {
        _overview: { state: true },
        _error: { state: true }
    };

    constructor() {
        super();
        this._overview = null;
        this._error = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getOverview().then(response => {
            this._overview = response.result;
        }).catch(error => {
            this._error = String(error);
        });
    }

    render() {
        return html`
            ${this._renderSite()}
            ${this._renderOutputs()}
            ${this._renderSecurityTxt()}
        `;
    }

    _renderSite() {
        if (this._error) {
            return html`<div class="error">${this._error}</div>`;
        }
        if (!this._overview) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        const o = this._overview;
        return html`
            <section>
                <h3>Site</h3>
                <table>
                    <tr><th>Site URL</th><td>${o.siteUrl}</td></tr>
                    <tr><th>Site name</th><td>${o.siteName}</td></tr>
                    <tr><th>Locales</th><td>${(o.locales || []).join(', ')}</td></tr>
                    <tr><th>Default locale</th><td>${o.defaultLocale}</td></tr>
                    <tr><th>Indexable</th><td>${o.indexable ? 'yes' : 'no'}</td></tr>
                </table>
            </section>
        `;
    }

    _renderOutputs() {
        return html`
            <section>
                <h3>Outputs</h3>
                <table>
                    <tr><th>Output</th><th>Path</th><th>Served by</th></tr>
                    ${(outputs || []).map(row => html`
                        <tr class="${row.servedBy === 'off' ? 'off' : ''}">
                            <td>${row.name}</td>
                            <td>${this._renderPath(row)}</td>
                            <td>${row.servedBy}</td>
                        </tr>
                    `)}
                </table>
            </section>
        `;
    }

    _renderPath(row) {
        const path = row.path || '';
        if (row.servedBy !== 'off' && path.startsWith('/') && !path.startsWith('//')) {
            return html`<a href="${path}" target="_blank" rel="noopener noreferrer">${path}</a>`;
        }
        return html`${path}`;
    }

    _renderSecurityTxt() {
        if (!this._overview) {
            return html``;
        }
        const warnings = this._overview.securityTxtWarnings || [];
        if (warnings.length === 0) {
            return html``;
        }
        return html`
            <section>
                <h3>security.txt</h3>
                <ul>
                    ${warnings.map(w => html`<li class="warning">${w.message} [${w.code}]</li>`)}
                </ul>
            </section>
        `;
    }
}
customElements.define('qwc-seo-overview', QwcSeoOverview);
