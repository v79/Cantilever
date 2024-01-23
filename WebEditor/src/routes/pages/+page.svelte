<script lang="ts">
	import { userStore } from '$lib/stores/userStore.svelte';
</script>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.isLoggedIn()}
			<h3 class="h3">Pages</h3>
            {:else}
			<p class="text-error-500">Not logged in</p>
		{/if}
	</div>
</div>
