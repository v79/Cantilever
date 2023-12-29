<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import { onDestroy, onMount } from 'svelte';
	import ActiveStoreView from '../../components/activeStoreView.svelte';
	import SpinnerWrapper, { spinnerStore } from '../../components/utilities/spinnerWrapper.svelte';
	import { ImageDTO } from '../../models/structure';
	import { AS_CLEAR, activeStore } from '../../stores/appStatusStore.svelte';
	import {
		addImage,
		allImagesStore,
		deleteImage,
		fetchImage,
		fetchImages,
		imageStore
	} from '../../stores/mediaStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import { Dropzone, Modal } from 'flowbite-svelte';

	afterNavigate(() => {
		activeStore.set(AS_CLEAR);
		$activeStore.currentPage = 'Media';
	});

	let hoveredImage: string = '';
	let showDeleteModal = false;
	let fileUploads: File[] = [];
	let confirmUpload = false;

	onMount(async () => {});

	/**
	 * Load
	 */
	export function loadAllImages() {
		console.log('Loading all images into store...');
		let token = $userStore.token;
		notificationStore.set({ shown: false, message: '', type: 'info' });

		let result = fetchImages(token);
		if (result) {
			// is error condition
			notificationStore.set({
				message: result.message,
				shown: true,
				type: 'error'
			});
			$spinnerStore.shown = false;
		} else {
			$notificationStore.message = 'Loaded all images ' + $activeStore.activeFile;
			$notificationStore.shown = true;
			$spinnerStore.shown = false;
		}
	}

	function requestImageDeletion() {
		console.log('Delete image ' + hoveredImage);
		showDeleteModal = false;
		$spinnerStore.message = 'Deleting image ' + hoveredImage;
		$spinnerStore.shown = true;
		deleteImage($userStore.token, hoveredImage);
		$allImagesStore.count--;
		$allImagesStore.images = $allImagesStore.images.filter((image) => {
			return image.key != hoveredImage;
		});
		$spinnerStore.shown = false;
	}

	function uploadImage() {
		$spinnerStore.message = 'Uploading image ' + fileUploads[0].name;
		$spinnerStore.shown = true;
		console.log('Upload image ' + fileUploads[0]);
		addImage($userStore.token, fileUploads[0]);
		fileUploads = [];
		confirmUpload = false;
		$spinnerStore.shown = false;
	}

	const userStoreUnsubscribe = userStore.subscribe((data) => {
		if (data) {
			loadAllImages();
		}
	});

	const imageStoreUnsubscribe = allImagesStore.subscribe((data) => {
		imageStore.set(data.images);
		// loop round each of the images in the store and asynchronously fetch the bytes
		for (const image of data.images) {
			fetchImage($userStore.token, image.key, '__thumb')
				.then((data) => {
					if (data instanceof Error) {
						throw new Error(data.message);
					}
					if (data instanceof ImageDTO) {
						let imageDiv = document.getElementById('img-' + data.srcKey);
						if (imageDiv) {
							imageDiv.appendChild(document.createElement('img'));
							let imgElement = imageDiv.getElementsByTagName('img')[0];
							if (imgElement) {
								imgElement.src = 'data:' + data.contentType + ';base64,' + data.bytes;
							}
						}
					} else {
						throw new Error('Unknown data type, expected ImageDTO');
					}
				})
				.catch((error) => {
					console.log(error);
					return;
				});
		}
	});

	const dropHandle = (event: DragEvent) => {
		fileUploads = [];
		event.preventDefault();
		if (event && event.dataTransfer) {
			if (event.dataTransfer.items) {
				[...event.dataTransfer.items].forEach((item, i) => {
					if (item.kind === 'file') {
						const file = item.getAsFile();
						if (file) {
							fileUploads.push(file);
							fileUploads = fileUploads;
							confirmUpload = true;
						}
					}
				});
			} else {
				[...event.dataTransfer.files].forEach((file, i) => {
					fileUploads.push(file);
				});
			}
		}
	};

	const handleUploadChange = (event: Event) => {
		if (event && event.target) {
			const files = event.target.files;
			if (files.length > 0) {
				fileUploads.push(files[0].name);
				fileUploads = fileUploads;
				confirmUpload = true;
			}
		}
	};

	onDestroy(userStoreUnsubscribe);
	onDestroy(imageStoreUnsubscribe);
</script>

<div class="flex grow flex-row">
	<div class="basis-3/4 bg-slate-600">
		<div class="relative mt-5 md:col-span-2 md:mt-0">
			<h3 class="px-4 py-4 text-center text-2xl font-bold">
				{#if $userStore == undefined}Login to see images{:else}Media Store{/if}

				{#if $allImagesStore.count > 0}
					<span class="text-sm text-gray-400">
						({$allImagesStore.count} images)
					</span>
				{/if}
			</h3>
			<div>
				<div class="ml-20 mr-10 grid grid-cols-4 gap-4">
					<Dropzone
						defaultClass="flex flex-col justify-center items-center w-full h-32 rounded-lg border-2 border-gray-300 border-dashed cursor-pointer dark:hover:bg-bray-800 dark:bg-gray-700 hover:bg-slate-400 dark:border-gray-600 dark:hover:border-gray-500 dark:hover:bg-gray-600"
						accept="image/*"
						on:drop={dropHandle}
						on:dragover={(event) => {
							event.preventDefault();
						}}
						on:click={(event) => {
							event.preventDefault();
						}}
						on:change={handleUploadChange}>
						<div class="relative flex flex-col items-center justify-center border border-slate-600">
							{#if fileUploads.length == 0}
								<p class="flex-grow text-lg font-bold text-white">Drop image here</p>
								<p class="flex-grow text-white">PNG, JPG, GIF or WEBP</p>
							{:else}
								<p class="flex-grow text-lg font-bold text-white">Confirm upload</p>
								<p class="flex-grow text-white">
									{fileUploads[0].name} ({fileUploads[0].size} bytes)
								</p>
							{/if}
							{#if confirmUpload}
								<div class="absolute bottom-2 z-10 rounded-sm bg-slate-200 opacity-75">
									<button
										class="btn btn-primary text-lg"
										on:click={() => {
											uploadImage();
										}}>✅</button>

									<button
										class="btn btn-secondary text-lg"
										on:click={() => {
											confirmUpload = false;
											fileUploads = [];
										}}>🗑️</button>
								</div>
							{/if}
						</div>
					</Dropzone>
					{#if $allImagesStore.count > 0}
						{#each $allImagesStore.images as image}
							<div
								id="img-{image.key}"
								on:mouseover={(event) => (hoveredImage = image.key)}
								on:focus={(event) => {}}
								class="relative flex flex-col items-center justify-center border border-slate-600 hover:border-white">
								<div class="flex-grow text-lg font-bold text-white">
									{image.shortName()}
								</div>
								<div class="flex-grow">
									{image.url}
									<img src="" alt={image.url} />
								</div>
								{#if hoveredImage == image.key}
									<div
										id="img-hover-controls"
										class="absolute bottom-2 rounded-sm bg-slate-200 opacity-75">
										<button
											class="btn btn-primary"
											on:click={() => {
												showDeleteModal = true;
											}}>🗑️</button>
									</div>
								{/if}
							</div>
						{/each}
					{/if}
				</div>
			</div>
		</div>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<ActiveStoreView />
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>

<Modal title="Delete image?" bind:open={showDeleteModal} autoclose size="sm">
	<p>
		Really delete image resolution named '{hoveredImage}'? This will delete the source file and the
		pre-generated image resolutions, but it will not remove the image from the published website.
	</p>
	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={requestImageDeletion}
			class="rounded bg-red-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Delete</button>
	</svelte:fragment>
</Modal>
