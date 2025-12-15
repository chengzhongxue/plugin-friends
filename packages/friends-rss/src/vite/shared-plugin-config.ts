import presetIcons from '@unocss/preset-icons';
import { presetUno } from 'unocss';
import UnoCSS from 'unocss/vite';

export const sharedPluginsConfig = [
  UnoCSS({
    mode: 'shadow-dom',
    presets: [presetUno(), presetIcons()],
    shortcuts: {
      'text-title': 'text-[var(--friends-rss-title-color,#18181b)]',
      'text-description': 'text-[var(--friends-rss-description-color,#71717a)]',
    },
  }),
];
