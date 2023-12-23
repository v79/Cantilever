<script lang="ts">
	import { onDestroy, onMount, tick } from 'svelte';
	import { afterNavigate } from '$app/navigation';
	import ActiveStoreView from '../../components/activeStoreView.svelte';
	import SpinnerWrapper from '../../components/utilities/spinnerWrapper.svelte';
	import { AS_CLEAR, activeStore } from '../../stores/appStatusStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import { spinnerStore } from '../../components/utilities/spinnerWrapper.svelte';
	import MediaStore, {
		allImagesStore,
		fetchImages,
		imageStore
	} from '../../stores/mediaStore.svelte';

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
		</div>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<ActiveStoreView />
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>
