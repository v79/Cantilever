<script lang="ts">
	import { ImageDTO, ImageNode } from '$lib/models/media';
	import { fetchImageBytes, fetchImages, images, uploadImage } from '$lib/stores/mediaStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		FileDropzone,
		getModalStore,
		getToastStore,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { Cancel, Done, Icon, Upload_file } from 'svelte-google-materialdesign-icons';

	let files: FileList | undefined = undefined; // for image uploads
	let iconHover = false; // for cross and tick icons on image uploads
	$: iconHovered = iconHover ? 'opacity-100' : 'opacity-100';
	const modalStore = getModalStore();
	const toastStore = getToastStore();

	const toast: ToastSettings = {
		message: 'Loaded posts',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to load posts',
		background: 'variant-filled-error'
	};

	onMount(async () => {
		if (!$images || $images.count <= 0) {
			await loadImages();
		}
	});

	async function loadImages() {
		if (!$userStore.token) {
			console.log('no token');
			return;
		} else {
			console.log('fetching images');
			const imageCount = await fetchImages($userStore.token);
			if (imageCount instanceof Error) {
				errorToast.message = 'Failed to load images. Message was: ' + imageCount.message;
				toastStore.trigger(errorToast);
				console.error(imageCount);
			} else {
				toast.message = `Loaded ${imageCount} images`;
				toastStore.trigger(toast);
			}
		}
	}

	// load the bytes of an image as an ImageDTO
	async function loadImageBytes(image: ImageNode): Promise<ImageDTO | Error> {
		console.log('loading thumbnail bytes for ' + image.srcKey);
		const imageBytes = await fetchImageBytes(image.srcKey, '__thumb', $userStore.token!!);
		if (imageBytes instanceof Error) {
			errorToast.message = 'Failed to load thumbnail bytes. Message was: ' + imageBytes.message;
			toastStore.trigger(errorToast);
			return imageBytes;
		} else if (imageBytes instanceof ImageDTO) {
			return imageBytes;
		} else {
			console.error(
				'Unknown error fetching thumbnail bytes for ' +
					image.srcKey +
					'; result was not an error or an ImageDTO'
			);
			return new Error(
				'Unknown error fetching thumbnail bytes for ' +
					image.srcKey +
					'; result was not an error or an ImageDTO'
			);
		}
	}

	// place an image in the DOM
	async function placeImage(image: ImageDTO) {
		let imageDiv = document.getElementById('img-' + image.srcKey);
		if (imageDiv) {
			imageDiv.appendChild(document.createElement('img'));
			let imgElement = imageDiv.getElementsByTagName('img')[0];
			if (imgElement) {
				imgElement.width = 100;
				imgElement.height = 100;
				imgElement.src = 'data:' + image.contentType + ';base64,' + image.bytes;
			}
		} else {
			console.log('Could not find div for image ' + image.srcKey);
		}
	}

	// respond to file drag events
	function dropzoneChangeHandler(e: Event) {
		if (files === undefined) {
			document.getElementById('dropPreview')?.setAttribute('src', '');
		} else {
			document.getElementById('dropPreview')?.setAttribute('src', URL.createObjectURL(files!![0]));
		}
	}

	function resetDropzone() {
		console.log('resetting dropzone');
		files = undefined;
	}

	async function intiateImageUpload() {
		if (files === undefined) {
			console.log('no files to upload');
			return;
		}
		if (!$userStore.token) {
			console.log('no token');
			return;
		}
		const response = uploadImage(files[0], $userStore.token);
		response.then(async (result) => {
			if (result instanceof Error) {
				errorToast.message = 'Failed to upload image. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				console.error(result);
			} else if (result instanceof ImageDTO) {
				toast.message = 'Uploaded image ' + result.srcKey + ' successfully';
				toastStore.trigger(toast);
				// it wil be a while before the image is available, so just place the DTO in the DOM
				// but at this stage, there is no dom element to place it in
				// so either I need to update the store, which will trigger load and then fail
				// or i need to create a dom element for it to be placed in
				// but will that mess things up when the store is updated?
				await placeImage(result);
				resetDropzone();
			} else {
				console.error('Unknown error uploading image; result was not an error or an ImageDTO');
			}
		});
	}

	const imagesUnsubscribe = images.subscribe(async (value) => {
		if (value && value.count != -1) {
			for (const imageFile of value.images) {
				const loadedImage = await loadImageBytes(imageFile);
				if (loadedImage instanceof ImageDTO) {
					await placeImage(loadedImage);
				} else {
					console.error(loadedImage);
				}
			}
		}
	});
</script>

<div class="flex flex-col grow mt-2 container">
	{#if $userStore.isLoggedIn()}
		<div class="flex flex-row justify-center w-full">
			<h3 class="h3 mb-2 text-center">Media</h3>
		</div>
		<div class="flex flex-col grow">
			<h3 class="h3 mb-2">
				{#if $images}{$images.count}{:else}Zero{/if} images
			</h3>
			<hr class="!border-t-2" />
			<div class="mr-10 mt-4 grid grid-cols-4 gap-4 relative z-0">
				<div class="relative flex flex-col">
					{#if files}
						<div class="absolute inset-0 flex justify-between items-center z-10 ml-2 mr-2">
							<button
								type="button"
								class="rounded-full hover:bg-gray-200 transition-colors duration-300 ease-in-out"
								on:click={resetDropzone}>
								<Icon icon={Cancel} color="red" size={48} variation="filled" />
							</button>
							<button
								type="button"
								class="rounded-full hover:bg-gray-200 transition-colors duration-300 ease-in-out"
								on:click={intiateImageUpload}>
								<Icon icon={Done} color="green" size={48} variation="filled" /></button>
						</div>
					{/if}
					<FileDropzone accept="image/*" bind:files name="files" on:change={dropzoneChangeHandler}>
						<svelte:fragment slot="lead">
							<div class="inline-block">
								{#if files === undefined}<Icon icon={Upload_file} size={36} />{:else}<img
										id="dropPreview"
										width="100"
										height="100"
										alt="preview of uploaded file" />{/if}
							</div>
						</svelte:fragment>
						<svelte:fragment slot="message"
							>{#if files === undefined}Upload an image file, or drag-and-drop{:else}{files[0]
									.name}{/if}</svelte:fragment>
						<svelte:fragment slot="meta"
							>{#if files === undefined}JPG, PNG, GIF and WEBP supported{:else}{files[0].type} ({files[0]
									.size} bytes){/if}</svelte:fragment>
					</FileDropzone>
				</div>
				{#if $images && $images.count > 0}
					{#each $images.images as image}
						<div
							id="img-{image.srcKey}"
							class="relative flex flex-col pb-2 items-center justify-between border border-slate-600 hover:border-white">
							<div class="text-lg font-bold text-white">
								{image.shortName()}
							</div>
						</div>
					{/each}
				{:else}
					<p>No images found</p>
				{/if}
			</div>
		</div>
	{/if}
</div>
