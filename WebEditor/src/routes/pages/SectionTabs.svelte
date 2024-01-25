<script lang="ts">
	import MarkdownEditor from '$lib/forms/markdownEditor.svelte';
	import { Tab, TabGroup } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';

	export let sections: Map<string, string>;
	let tabGroup = '';

	onMount(() => {
		tabGroup = Object.keys(sections)[0];
	});

	let updateSection = (newBody: string) => {
        // @ts-ignore
		sections[tabGroup] = newBody;
	};
</script>

<TabGroup regionList="w-full">
	{#each Object.entries(sections) as [key, value]}
		<Tab bind:group={tabGroup} name={key} value={key}><span>{key}</span></Tab>
	{/each}
	<svelte:fragment slot="panel">
		{#if sections[tabGroup]}
			{@const body = sections[tabGroup]}
			<MarkdownEditor {body} onChange={updateSection} />
		{/if}
	</svelte:fragment>
</TabGroup>
