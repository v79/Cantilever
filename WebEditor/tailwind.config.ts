import { join } from 'path'
import type { Config } from 'tailwindcss'
import forms from '@tailwindcss/forms';
import typography from '@tailwindcss/typography';
const { skeleton } = require('@skeletonlabs/tw-plugin');
import { Cantilever } from './src/Cantilever'

export default {
	darkMode: 'class',
	content: ['./src/**/*.{html,js,svelte,ts}', join(require.resolve('@skeletonlabs/skeleton'), '../**/*.{html,js,svelte,ts}')],
	theme: {
		extend: {},
	},
	plugins: [
		forms,
		typography,
		skeleton({
			themes: {
				preset: [
					{
						name: 'wintry',
						enhancements: true,
					},
				],
				custom: [
					Cantilever,
				],
			},
		}),
	],
} satisfies Config;
