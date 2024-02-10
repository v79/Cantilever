<script lang="ts">
	import { userStore } from '$lib/stores/userStore.svelte';

	$: isLoggedIn = $userStore.isLoggedIn();
</script>

<svelte:head>
	<title>Cantilever</title>
</svelte:head>

<div class="flex flex-col grow mt-2 container">
	{#if isLoggedIn}
		<div class="flex flex-row justify-center w-full">
			<h3 class="h3 mb-2 text-center">Welcome</h3>
		</div>
		<div class="flex flex-col grow">
			<p class="p">This is where I'd put a list of projects, if I had any.</p>
			<p class="p">Plus provide some shortcut buttons to features like...</p>
			<ul class="list-inside list-disc">
				<li>Creating a new page</li>
				<li>Creating a new post</li>
				<li>Regenerating all content</li>
			</ul>
		</div>
	{:else}
		<h3 class="h3">Please log in to continue</h3>
	{/if}
</div>
