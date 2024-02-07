<script lang="ts">
	import { ImageDTO, ImageNode } from '$lib/models/media';
	import {
		deleteImage,
		fetchImageBytes,
		fetchImages,
		images,
		uploadImage
	} from '$lib/stores/mediaStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		FileDropzone,
		getModalStore,
		getToastStore,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import { onMount, tick } from 'svelte';
	import { Cancel, Done, Icon, Upload_file } from 'svelte-google-materialdesign-icons';
	import DeleteForever from 'svelte-google-materialdesign-icons/Delete_forever.svelte';

	let files: FileList | undefined = undefined; // for image uploads
	let imageOverlayHover = false; // for delete image overlay
	$: hoveredImage = '';
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

	/**
	 * @type: {ModalSettings}
	 */
	$: deleteImageModal = {
		type: 'confirm',
		title: 'Delete Image',
		body: 'Are you sure you want to delete image <strong>' + hoveredImage + '</strong>?',
		buttonTextConfirm: 'Delete',
		buttonTextCancel: 'Cancel',
		response: (r: boolean) => {
			if (r) {
				initiateImageDelete(hoveredImage);
			}
			modalStore.close();
		}
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

	// reset the dropzone
	function resetDropzone() {
		console.log('resetting dropzone');
		files = undefined;
	}

	// trigger upload of image to server, and place it if successful
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
				console.log('Uploaded image ' + result.srcKey + ' successfully');
				toast.message = 'Uploaded image ' + result.srcKey + ' successfully';
				toastStore.trigger(toast);
				let bytesToPlace = (result.bytes as string).split(',')[1];
				let imageToPlace = new ImageDTO(result.srcKey, result.contentType, bytesToPlace);
				$images.images.push(new ImageNode(result.srcKey, new Date(), '', result.contentType, true));
				$images.count++;
				await tick();
				placeImage(imageToPlace);
				resetDropzone();
			} else {
				console.error('Unknown error uploading image; result was not an error or an ImageDTO');
			}
		});
	}

	// handle image hover
	function hoverImage(srcKey: string) {
		hoveredImage = srcKey;
		imageOverlayHover = true;
	}

	// trigger delete of image from server
	async function initiateImageDelete(srcKey: string) {
		console.log('deleting image ' + srcKey);
		const deleteResponse = await deleteImage(srcKey, $userStore.token!!);
		if (deleteResponse instanceof Error) {
			errorToast.message = 'Failed to delete image. Message was: ' + deleteResponse.message;
			toastStore.trigger(errorToast);
			console.error(deleteResponse);
		} else {
			toast.message = 'Deleted image ' + srcKey + ' successfully';
			toastStore.trigger(toast);
			
			$images.images.filter((image) => image.srcKey !== srcKey);
			loadImages();
		}
	}

	const imagesUnsubscribe = images.subscribe(async (value) => {
		if (value && value.count != -1) {
			for (const imageFile of value.images) {
				if (!imageFile.hasBeenPlaced) {
					const loadedImage = await loadImageBytes(imageFile);
					if (loadedImage instanceof ImageDTO) {
						await placeImage(loadedImage);
						imageFile.hasBeenPlaced = true;
					} else {
						console.error(loadedImage);
					}
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
				{#if $images}{$images.count}{:else}Loading{/if} images
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
							role="tooltip"
							on:mouseover={() => {
								hoverImage(image.srcKey);
							}}
							on:focus={() => {
								hoverImage(image.srcKey);
							}}
							id="img-{image.srcKey}"
							class="relative flex flex-col pb-2 items-center justify-around border border-slate-600 hover:border-white">
							<div class="text-lg font-bold text-white">
								{image.shortName()}
							</div>
							{#if hoveredImage === image.srcKey}
								<div
									class="absolute flex flex-col place-items-end inset-0 justify-end z-10 ml-2 mr-2">
									<button
										type="button"
										class="rounded-full hover:bg-gray-200 transition-colors duration-300 ease-in-out"
										on:click={() => {
											modalStore.trigger(deleteImageModal);
										}}>
										<Icon icon={DeleteForever} color="red" size={36} variation="filled" />
									</button>
								</div>
							{/if}
						</div>
					{/each}
				{:else}
					<p>No images found</p>
				{/if}
			</div>
		</div>
	{/if}
</div>
