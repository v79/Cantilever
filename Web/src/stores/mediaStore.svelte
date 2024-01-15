<script lang="ts" context="module">
	/** This store manages the images and other media in the project */
	import { writable } from 'svelte/store';
	import { notificationStore } from './notificationStore.svelte';
	import type { AllImages } from '../models/structure';
	import { ImageDTO, MediaImage } from '../models/structure';

	export const allImagesStore = writable<AllImages>({
		count: 0,
		lastUpdated: new Date(),
		images: []
	});
	export const imageStore = writable<MediaImage[]>();

	export const IMAGES_CLEAR: AllImages = { count: 0, lastUpdated: new Date(), images: [] };

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
				if (data.data === undefined) {
					throw new Error(data.message);
				}
				// deserialize
				var tempImages = new Array<MediaImage>();
				for (const i of data.data.images) {
					tempImages.push(new MediaImage(i.srcKey, i.lastUpdated, i.url, false));
				}

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

	/** Asynchronously fetch the bytes of an image
	 * @param token authentication token
	 * @param srcKey the key of the image to fetch
	 * @param resolution the resolution of the image to fetch
	 */
	export async function fetchImage(
		token: string,
		srcKey: string,
		resolution: string | undefined
	): Promise<Error | ImageDTO | undefined> {
		// console.log(`Loading image bytes for ${srcKey} at ${resolution}...`);
		notificationStore.set({ shown: false, message: '', type: 'info' });
		let encodedKey = encodeURIComponent(srcKey);
		try {
			const response = await fetch(
				'https://api.cantilevers.org/media/images/' + encodedKey + '/' + resolution,
				{
					method: 'GET',
					headers: {
						Accept: 'application/json',
						Authorization: 'Bearer ' + token
					},
					mode: 'cors'
				}
			);

			const data = await response.json();
			if (data.data === undefined) {
				throw new Error(data.message);
			}

			// deserialize
			const tempImage = new ImageDTO(
				decodeURIComponent(data.data.srcKey),
				data.data.contentType,
				data.data.bytes
			);

			return tempImage;
		} catch (error) {
			console.log(error);
			return error as Error;
		}
	}

	export async function addImage(token: string, file: File): Promise<Error | ImageDTO> {
		const reader = new FileReader();
		reader.readAsDataURL(file);
		return new Promise((resolve, reject) => {
			reader.onload = () => {
				// base64 encode the bytes in file
				const dto = new ImageDTO(file.name, file.type, reader.result as string);
				let dtoString = JSON.stringify(dto);

				try {
					const response = fetch('https://api.cantilevers.org/media/images/', {
						method: 'POST',
						headers: {
							Accept: 'application/json',
							Authorization: 'Bearer ' + token,
							'Content-Type': 'application/json'
						},
						mode: 'cors',
						body: dtoString
					})
						.then((response) => response.json())
						.then((data) => {
							// we set the bytes to the result of the reader, rather than from the API result, because we've already got the bytes
							let result = new ImageDTO(
								data.data.srcKey,
								data.data.contentType,
								reader.result as string
							);
							resolve(result);
						});
				} catch (error) {
					console.log(error);
					resolve(error as Error);
				}
			};
		});
	}

	export async function deleteImage(token: string, srcKey: string) {
		console.log('Deleting image ' + srcKey + '...');
		let encodedKey = encodeURIComponent(srcKey);
		try {
			const response = await fetch('https://api.cantilevers.org/media/images/' + encodedKey, {
				method: 'DELETE',
				headers: {
					Accept: 'text/plain',
					Authorization: 'Bearer ' + token
				},
				mode: 'cors'
			});
			const data = await response.text();
			console.log(data);
		} catch (error) {
			console.log(error);
			return error as Error;
		}
	}
</script>
