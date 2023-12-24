<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import { notificationStore } from './notificationStore.svelte';
	import type { AllImages } from '../models/structure';
	import { MediaImage } from '../models/structure';

	export const allImagesStore = writable<AllImages>({
		count: 0,
		lastUpdated: new Date(),
		images: []
	});
	export const imageStore = writable<MediaImage[]>();

	/**
	 * Populate the allImagesStore by fetching from the server
	 * @param token authentication token
	 */
	export function fetchImages(token: string): Error | undefined {
		console.log('Loading all images json...');
		notificationStore.set({ shown: false, message: '', type: 'info' });
		fetch('https://api.cantilevers.org/media/images', {
			method: 'GET',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				console.log('Loaded all images json');
				console.dir(data);
				if (data.data === undefined) {
					throw new Error(data.message);
				}
				// deserialize
				var tempImages = new Array<MediaImage>();
				for (const i of data.data.images) {
					tempImages.push(new MediaImage(i.srcKey, i.lastUpdated, i.url));
				}
				console.log('tempImages: ' + tempImages.length);
				console.log('count: ' + data.data.count);

				// set images store
				allImagesStore.set({
					count: data.data.count,
					lastUpdated: data.data.lastUpdated,
					images: tempImages
				});
			})
			.catch((error) => {
				console.log(error);
				return error;
			});
		return;
	}
</script>
