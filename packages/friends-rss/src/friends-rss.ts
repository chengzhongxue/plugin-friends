import resetStyles from '@unocss/reset/tailwind.css?inline';
import { LitElement, css, html, unsafeCSS } from 'lit';
import { property, state } from 'lit/decorators.js';
import { RssDetail } from './types';
import { repeat } from 'lit/directives/repeat.js';
import './loading-bar';

export class FriendsRss extends LitElement {
  @property({ type: String })
  src = '';

  @property({ type: String })
  layout: 'list' | 'grid' = 'list';

  @state()
  rssDetail?: RssDetail;

  @state()
  loading = false;

  override connectedCallback() {
    super.connectedCallback();
    this.fetchRssDetail();
  }

  async fetchRssDetail() {
    // Mock
    try {
      this.loading = true;
      const response = await fetch(
        `/apis/api.friend.moony.la/v1alpha1/parsingrss?rssUrl=${this.src}`
      );

      if (!response.ok) {
        throw new Error('Failed to fetch site data');
      }

      this.rssDetail = (await response.json()) as RssDetail;
    } catch (error) {
      console.error(error);
    } finally {
      this.loading = false;
    }
  }

  override render() {
    if (this.loading) {
      return html`<loading-bar></loading-bar>`;
    }

      return html`
          <ul class="grid ${this.layout == 'grid' ? 'grid-cols-2' : ''}">
              ${repeat(
                  // @ts-ignore
                  this.rssDetail?.channel.items,
                  (item) => item.link,
                  (item) => html`
                    <li class="sm:flex-row relative p-2 space-y-1 z-[1]">
                        <a
                            href=${item?.link}
                            target="_blank"
                        >
                            <h2 class="font-semibold text-base text-title line-clamp-2 lg:line-clamp-1">
                                ${item?.title}
                            </h2>
                        </a>
                        <p class="text-sm text-description line-clamp-2">${item?.description}</p>
                    </li>
                `
              )}
          </ul>
    `;
  }

  static override styles = [
    unsafeCSS(resetStyles),
    css`
      :host {
        display: inline-block;
        width: 100%;
      }
      @unocss-placeholder;
    `,
  ];
}

customElements.get('friends-rss') || customElements.define('friends-rss', FriendsRss);

declare global {
  interface HTMLElementTagNameMap {
    'friends-rss': FriendsRss;
  }
}
