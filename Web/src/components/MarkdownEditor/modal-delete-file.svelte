<script lang="ts">
	import { CLEAR, markdownStore } from '../../stores/markdownContentStore.svelte';
	import { Modal } from 'flowbite-svelte';
	import { spinnerStore } from '../utilities/spinnerWrapper.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import { allPostsStore } from '../../stores/postsStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import { createEventDispatcher } from 'svelte';

	export let shown = false;

	const closeDispatch = createEventDispatcher();
	function callDispatcher(e: MouseEvent) {
		shown = false;
		closeDispatch('closeModal');
	}

	const CONFIRM_DELETE = 'delete';
	let confirmed = false;
	let confirmInput = '';

	function deleteFile() {
		spinnerStore.set({ message: 'Deleting... ' + $markdownStore.metadata?.srcKey, shown: true });
		let srcKey = decodeURIComponent($markdownStore.metadata?.srcKey!!);
		console.log('Deleting file ', srcKey);
		fetch(
			'https://api.cantilevers.org/posts/' + encodeURIComponent($markdownStore.metadata?.srcKey!!),
			{
				method: 'DELETE',
				headers: {
					Accept: 'text/plain',
					Authorization: 'Bearer ' + $userStore.token
				},
				mode: 'cors'
			}
		)
			.then((response) => response.text())
			.then((data) => {
				notificationStore.set({
					message: data,
					shown: true,
					type: 'success'
				});
				$allPostsStore.count--;
				let toDelete = $allPostsStore.posts.findIndex((post) => post.srcKey === srcKey);
				$allPostsStore.posts.splice(toDelete, 1);
				markdownStore.set(CLEAR);
			})
			.catch((error) => {
				notificationStore.set({ message: 'Error deleting: ' + error, shown: true, type: 'error' });
				console.log(error);
			});
		$spinnerStore.shown = false;
	}

	function verify() {
		if (confirmInput === CONFIRM_DELETE) {
			confirmed = true;
		}
	}
</script>

<!-- Delete file modal-->
<Modal title="Delete file?" bind:open={shown} size="sm">
	<p>
		Delete source file <strong>{$markdownStore.metadata?.title}</strong>
		({decodeURIComponent($markdownStore.metadata.srcKey)})? Are you sure?
	</p>
	<p class="text-red-600">This cannot be undone! Type '{CONFIRM_DELETE}' to confirm.</p>
	<form>
		<input
			type="text"
			bind:value={confirmInput}
			on:keyup={verify}
			id="delete-confirm"
			name="delete-confirm"
			class="mt-1 block w-full rounded-md border-gray-300 text-slate-500 shadow-sm sm:text-sm" />
	</form>
	<svelte:fragment slot="footer">
		<button
			type="button"
			on:click={(e) => callDispatcher(e)}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			disabled={!confirmed}
			on:click={(e) => {
				shown = false;
				confirmInput = '';
				callDispatcher(e);
				deleteFile();
			}}
			class="rounded bg-red-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg disabled:bg-red-200"
			>Delete</button>
	</svelte:fragment>
</Modal>
