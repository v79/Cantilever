<script lang="ts" context="module">
	import { ImageNode, type ImageList, ImageDTO } from '$lib/models/media';
	import { writable } from 'svelte/store';

	function createImageStore() {
		const { subscribe, set, update } = writable<ImageList>();

		return {
			subscribe,
			set,
			update
		};
	}

	// This store manages media, such as images
	export const images = createImageStore();

	// fetch the list of images from the server and store in the images store
	export async function fetchImages(token: string): Promise<number | Error> {
		console.log('mediaStore: Fetching images');
		try {
			const response = await fetch('https://api.cantilevers.org/media/images', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				/** @type {ImageDTO} */
				const data = await response.json();
				// need to coerce the data to the correct type
				let tmpImages = new Array<ImageNode>();
				for (let i = 0; i < data.data.images.length; i++) {
					tmpImages.push(
						new ImageNode(
							data.data.images[i].srcKey,
							data.data.images[i].lastUpdated,
							data.data.images[i].url,
							data.data.images[i].contentType,
							false
						)
					);
				}

				images.set({ count: tmpImages.length, lastUpdated: new Date(), images: tmpImages });
				return data.data.count as number;
			} else {
				throw new Error('Failed to fetch images');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// fetch the bytes of an image from the server and return it as an ImageDTO
	export async function fetchImageBytes(
		srcKey: string,
		resolution: string,
		token: string
	): Promise<ImageDTO | Error> {
		try {
			let encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(
				`https://api.cantilevers.org/media/images/${encodedKey}/${resolution}`,
				{
					method: 'GET',
					headers: {
						Accept: 'application/json',
						Authorization: `Bearer ${token}`
					},
					mode: 'cors'
				}
			);
			if (response.ok) {
				const data = await response.json();
				// deserialize
				const tempImage = new ImageDTO(
					decodeURIComponent(data.data.srcKey),
					data.data.contentType,
					data.data.bytes
				);
				return tempImage;
			} else {
				throw new Error('Failed to fetch image bytes');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// upload an image to the server
	export async function uploadImage(file: File, token: string): Promise<Error | ImageDTO> {
		try {
			const reader = new FileReader();
			reader.readAsDataURL(file);
			return new Promise((resolve, reject) => {
				reader.onload = () => {
					// base64 encode the bytes in file
					const dto = new ImageDTO(file.name, file.type, reader.result as string);
					let dtoString = JSON.stringify(dto);

					const response = fetch('https://api.cantilevers.org/media/images/', {
						method: 'POST',
						headers: {
							Accept: 'application/json',
							Authorization: 'Bearer ' + token,
							'Content-Type': 'application/json'
						},
						body: dtoString
					});
					response
						.then((response) => response.json())
						.then((data) => {
							// we set the bytes to the result of the reader, rather than from the API result, because we've already got the bytes
							let result = new ImageDTO(
								data.data.srcKey,
								data.data.contentType,
								reader.result as string
							);
							resolve(result);
						})
						.catch((error) => {
							reject(new Error('Failed to upload image'));
						});
				};
			});
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
