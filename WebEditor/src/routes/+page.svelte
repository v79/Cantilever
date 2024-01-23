<script lang="ts">
	import { userStore } from '$lib/stores/userStore.svelte';

	$: isLoggedIn = $userStore.isLoggedIn();
</script>

{#if isLoggedIn}
	<h2 class="h2">Project configuration</h2>
{:else}
	<h3 class="h3">Please log in to continue</h3>
{/if}
