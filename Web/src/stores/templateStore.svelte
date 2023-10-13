<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import type {
		AllTemplates,
		Template as TemplateType,
		HandlebarsContent as HBContentType
	} from '../models/structure';
	import { Template, HandlebarsContent } from '../models/structure';
	import { compute_rest_props } from 'svelte/internal';

	export const allTemplatesStore = writable<AllTemplates>({
		count: 0,
		lastUpdated: new Date(),
		templates: []
	});
	export const templateStore = writable<Template[]>();

	/**
	 * Populate the allTemplatesStore by fetching from the server
	 * @param token authentication token
	 */
	export function fetchTemplates(token: string): Error | undefined {
		fetch('https://api.cantilevers.org/project/templates', {
			method: 'GET',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				if (data.data === undefined) {
					throw new Error(data.message);
				}
				// deserialize
				var tempTemplates = new Array<Template>();
				for (const t of data.data.templates) {
					tempTemplates.push(new Template(t.key, t.lastUpdated));
				}
				// set templates store
				allTemplatesStore.set({
					count: tempTemplates.length,
					lastUpdated: data.lastUpdated,
					templates: tempTemplates
				});
			})
			.catch((error: Error) => {
				console.log(error);
				return error;
			});
		return;
	}

	/**
	 * Fetch a handlebars template file for the given key
	 * @param token
	 * @param key
	 * @returns Promise<HBContentType | Error | undefined>
	 */
	export async function fetchHandlebarTemplate(
		token: string,
		key: string
	): Promise<HBContentType | Error | undefined> {
		console.log('Fetching template ' + key);
		let template: HBContentType | undefined = undefined;

		return new Promise((resolve) => {
			fetch('https://api.cantilevers.org/templates/' + encodeURIComponent(key), {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: 'Bearer ' + token
				},
				mode: 'cors'
			})
				.then((response) => response.json())
				.then((data) => {
					if (data === undefined) {
						throw new Error(data.message);
					}
					template = new HandlebarsContent(
						new Template(data.data.template.key, data.data.template.lastUpdated),
						data.data.body
					);
					console.log('Built handlebars template');
					resolve(template);
				})
				.catch((error: Error) => {
					console.log(error);
					resolve(error);
				});
		});
	}
</script>
