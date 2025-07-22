import { purgeCss } from 'vite-plugin-tailwind-purgecss';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

/**
 * proxy: {
			'/v1': {
				target: 'https://dev-api.cantilevers.org',
				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/v1/, '')
			}
		},
 */
export default defineConfig({
	server: {
		port: 5173,
		
	},
	preview: {
		port: 5173
	},
	plugins: [
		sveltekit(),
		purgeCss({
			safelist: {
				// any selectors that begin with "hljs-" will not be purged
				greedy: [/^hljs-/]
			}
		})
	]
});
