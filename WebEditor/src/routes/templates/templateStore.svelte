<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import type { TemplateList } from '../../models/templates.svelte';
	import { handlebars } from '../../stores/contentStore.svelte';

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
</script>
