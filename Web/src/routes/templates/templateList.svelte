<script lang="ts">
	import { onDestroy, onMount, tick } from 'svelte';
	import HandlebarListItem from '../../components/handlebarListItem.svelte';
	import { spinnerStore } from '../../components/utilities/spinnerWrapper.svelte';
	import { FileType, FolderNode, HandlebarsContent, HandlebarsItem, Post, Template } from '../../models/structure';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import { handlebarStore } from '../../stores/handlebarContentStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import {
		allTemplatesStore,
		fetchHandlebarTemplate,
		templateStore
	} from '../../stores/templateStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import { fetchTemplates } from '../../stores/templateStore.svelte';

	$: templatesSorted = $templateStore.sort((a, b) => {
		if (a.key < b.key) return -1;
		if (a.key > b.key) return 1;
		return 0;
	});

	onMount(async () => {});

	/**
	 * Load
	 */
	export function loadAllTemplates() {
		console.log('Loading all templates json...');
		let token = $userStore.token;
		notificationStore.set({ shown: false, message: '', type: 'info' });

		let result = fetchTemplates(token);
		if (result) {
			// is error condition
			notificationStore.set({
				message: result.message,
				shown: true,
				type: 'error'
			});
			$spinnerStore.shown = false;
		} else {
			$notificationStore.message = 'Loaded all templates ' + $activeStore.activeFile;
			$notificationStore.shown = true;
			$spinnerStore.shown = false;
		}
	}

	function createNewTemplate() {
		console.log('Create new template');
	}

	async function rebuild() {
		let token = $userStore.token;
		console.log('Regenerating project templates file...');
		fetch('https://api.cantilevers.org/project/templates/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token,
				'X-Content-Length': '0'
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				console.log(data);
				notificationStore.set({
					message: data.data,
					shown: true,
					type: 'success'
				});
				loadAllTemplates();
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
			});
	}

	/**
	 * Load the handlebars template file into the activeStore
	 * @param key
	 */
	async function loadHandlebars(key: string) {
		let token = $userStore.token;
		console.log('Loading handlebars file... ' + key);
		spinnerStore.set({ shown: true, message: 'Loading handlebars file... ' + key });
		notificationStore.set({ shown: false, message: '', type: 'info' });
		tick();

		fetchHandlebarTemplate(token, key).then((response) => {
			console.log('Fetched');
			console.dir(response);
			if (response instanceof Error) {
				notificationStore.set({
					message: response.message,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
			} else if (response instanceof HandlebarsContent) {
				handlebarStore.set(response);
				$activeStore.activeFile = decodeURIComponent($handlebarStore.template!!.key);
				$activeStore.isNewFile = false;
				$activeStore.hasChanged = false;
				$activeStore.isValid = true;
				$activeStore.newSlug = "";
				$activeStore.fileType = FileType.Template;
				$activeStore.folder = new FolderNode("folder","sources/templates/",0,[]);
				$notificationStore.message = 'Loaded file ' + $activeStore.activeFile;
				$notificationStore.shown = true;
				$spinnerStore.shown = false;
			} else {
				console.log('Failed to fetch template ' + key + ' and no error was returned!');
			}
		});
	}

	const userStoreUnsubscribe = userStore.subscribe((data) => {
		if (data) {
			loadAllTemplates();
		}
	});

	const templateStoreUnsubscribe = allTemplatesStore.subscribe((data) => {
		templateStore.set(data.templates);
	});

	onDestroy(userStoreUnsubscribe);
	onDestroy(templateStoreUnsubscribe);
</script>

<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-900">Templates</h3>

{#if $userStore === undefined}
	<div class="px-8"><p class="text-warning text-lg">Login to see templates</p></div>
{:else}
	<div class="flex items-center justify-center" role="group">
		<button
			class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={(e) => {
				console.log('Show spinner');
				spinnerStore.set({ shown: true, message: 'Rebuilding project...' });
				tick().then(() => rebuild());
			}}>Rebuild</button>
		<button
			class="inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={(e) => {
				console.log('Show spinner');
				spinnerStore.set({ shown: true, message: 'Reloading project...' });
				tick().then(() => loadAllTemplates());
			}}>Reload</button>
		<button
			type="button"
			on:click={createNewTemplate}
			class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			>New Template</button>
	</div>
	<div class="px-8">
		{#if allTemplatesStore}
			<h4 class="text-right text-sm text-slate-900">{$allTemplatesStore.count} templates</h4>
		{/if}
		{#if templatesSorted.length > 0}
			<div class="justify-left flex py-2">
				<ul class="w-96 rounded-lg border border-gray-400 bg-white text-slate-900">
					{#each templatesSorted as template}
						<HandlebarListItem
							item={new HandlebarsItem(template.key, template.lastUpdated)}
							onClickFn={loadHandlebars} />
					{/each}
				</ul>
			</div>
		{:else}
			<div class="flex items-center justify-center py-4">
				<div class="flex min-h-screen w-full items-center justify-center bg-gray-200">
					<svg
						class="h-12 w-12 animate-spin text-indigo-400"
						viewBox="0 0 24 24"
						fill="none"
						xmlns="http://www.w3.org/2000/svg">
						<path
							d="M12 4.75V6.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M17.1266 6.87347L16.0659 7.93413"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M19.25 12L17.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M17.1266 17.1265L16.0659 16.0659"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M12 17.75V19.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M7.9342 16.0659L6.87354 17.1265"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M6.25 12L4.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M7.9342 7.93413L6.87354 6.87347"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
					</svg>
				</div>
			</div>
		{/if}
	</div>
{/if}
