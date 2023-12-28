<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import { onDestroy, onMount } from 'svelte';
	import ActiveStoreView from '../../components/activeStoreView.svelte';
	import SpinnerWrapper, { spinnerStore } from '../../components/utilities/spinnerWrapper.svelte';
	import { ImageDTO } from '../../models/structure';
	import { AS_CLEAR, activeStore } from '../../stores/appStatusStore.svelte';
	import {
		allImagesStore,
		fetchImage,
		fetchImages,
		imageStore
	} from '../../stores/mediaStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';

	afterNavigate(() => {
		activeStore.set(AS_CLEAR);
		$activeStore.currentPage = 'Media';
	});

	function mapReplacer(key: string, value: any): any {
		if (value instanceof Map) {
			return Object.fromEntries(value);
		} else {
			return value;
		}
	}

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
						console.log('Pushed image onto imageDTOs');
						let imageDiv = document.getElementById('img-' + data.key);
						if (imageDiv) {
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
				{#if $allImagesStore.count > 0}
					<div class="ml-20 mr-10 grid grid-cols-4 gap-4">
						{#each $allImagesStore.images as image}
							<div
								id="img-{image.key}"
								class="flex flex-col items-center justify-center hover:border hover:border-white">
								<div class="flex-grow text-lg font-bold text-white">
									{image.shortName()}
								</div>
								<div class="flex-grow">
									{image.url}
									<img src="" alt={image.url} />
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>
		</div>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<ActiveStoreView />
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>
