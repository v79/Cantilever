<script lang="ts">
	import { ImageDTO, type ImageNode } from '$lib/models/media';
	import { fetchImageBytes, fetchImages, images } from '$lib/stores/mediaStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { getModalStore, getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

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
		if (!$images) {
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

	// load the bytes of an image as an ImageDTO and place it in the DOM
	function loadImageBytes(image: ImageNode) {
		console.log('loading image bytes for ' + image.srcKey);
		const imageBytes = fetchImageBytes(image.srcKey, '__thumb', $userStore.token!!);
		imageBytes.then((result) => {
			if (result instanceof Error) {
				errorToast.message = 'Failed to load image bytes. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				console.error(result);
			} else if (result instanceof ImageDTO) {
				placeImage(result);
			} else {
				console.error('Unknown error fetching image bytes; result was not an error or an ImageDTO');
			}
		});
	}

	// place an image in the DOM
	function placeImage(image: ImageDTO) {
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
	const imagesUnsubscribe = images.subscribe((value) => {
		if (value && value.count != -1) {
			for (const imageFile of value.images) {
				loadImageBytes(imageFile);
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
			{#if $images && $images.count > 0}
				<h3 class="h3 mb-2">{$images.count} images</h3>
				<hr class="!border-t-2"/>

				<div class="mr-10 mt-4 grid grid-cols-4 gap-4">
					{#each $images.images as image}
						<div
							id="img-{image.srcKey}"
							class="relative flex flex-col pb-2 items-center justify-center border border-slate-600 hover:border-white">
							<div class="text-lg font-bold text-white">
								{image.shortName()}
							</div>
						</div>
					{/each}
				</div>
			{:else}
				<p>No images found</p>
			{/if}
		</div>
	{/if}
</div>
