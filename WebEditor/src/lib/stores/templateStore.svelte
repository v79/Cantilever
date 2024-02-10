<script lang="ts" context="module">
	import { writable, get } from 'svelte/store';
	import type { TemplateList, TemplateNode, TemplateUsageDTO } from '$lib/models/templates.svelte';
	import { handlebars } from '$lib/stores/contentStore.svelte';

	// complete set of template metadata
	export const templates = writable<TemplateList>();

	// fetch list of templates from server
	export async function fetchTemplates(token: string): Promise<number | Error> {
		console.log('templateStore: Fetching templates');
		try {
			const response = await fetch('https://api.cantilevers.org/templates', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				templates.set(data.data);
				return data.data.count as number;
			} else {
				throw new Error('Failed to fetch templates');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// fetch template from server
	export async function fetchTemplate(srcKey: string, token: string): Promise<Error | string> {
		console.log('templateStore: Fetching template', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/templates/${encodedKey}`, {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				// console.log('templateStore: Fetched template', data.data);
				handlebars.set(data.data);
				return 'Loaded template' + srcKey;
			} else {
				throw new Error('Failed to fetch template');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// save template to server
	export async function saveTemplate(token: string): Promise<Error | string> {
		let template: TemplateNode = get(handlebars);
		console.log('templateStore: Saving template', template.srcKey);
		try {
			const response = await fetch('https://api.cantilevers.org/templates/save', {
				method: 'POST',
				headers: {
					Accept: 'text/plain',
					Authorization: `Bearer ${token}`,
					'Content-Type': 'application/json'
				},
				mode: 'cors',
				body: JSON.stringify(template)
			});
			if (response.ok) {
				const message = await response.text();
				console.log('templateStore: Saved template', message);
				templates.update((value) => {
					if (value) {
						let index = value.templates.findIndex((t) => t.srcKey === template.srcKey);
						if (index >= 0) {
							value.templates[index] = template;
						} else {
							value.templates.push(template);
						}
					}
					return value;
				});
				return message;
			} else {
				throw new Error('Failed to save template');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// fetch a list of the pages and posts which use a given template
	export async function fetchTemplateUsage(
		srcKey: string,
		token: string
	): Promise<Error | TemplateUsageDTO> {
		console.log('templateStore: Fetching template usage', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/templates/usage/${encodedKey}`, {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				return data.data as TemplateUsageDTO;
			} else {
				throw new Error('Failed to fetch template usage');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// delete template from server
	export async function deleteTemplate(srcKey: string, token: string): Promise<Error | string> {
		console.log('templateStore: Deleting template', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/templates/${encodedKey}`, {
				method: 'DELETE',
				headers: {
					Accept: 'text/plain',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const message = await response.text();
				console.log('templateStore: Deleted template', message);
				templates.update((value) => {
					if (value) {
						value.templates = value.templates.filter((t) => t.srcKey !== srcKey);
					}
					return value;
				});
				return message;
			} else {
				throw new Error('Failed to delete template');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// trigger generation of pages with the given template
	export async function regenerate(srcKey: string, token: string): Promise<Error | string> {
		console.log('templateStore: Regenerating content for template ', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/generate/template/${encodedKey}`, {
				method: 'PUT',
				headers: {
					Accept: 'text/plain',
					Authorization: `Bearer ${token}`,
					'X-Content-Length': '0'
				},
				mode: 'cors'
			});
			if (response.ok) {
				const message = await response.text();
				console.log('templateStore: Regenerated pages', message);
				return message;
			} else {
				throw new Error('Failed to regenerate from template');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
