<script>
	import '../app.css';
	import { onMount } from 'svelte';

	onMount(async () => {
		await import('tw-elements/dist/src/js/index.js');
	});
</script>

<slot />
