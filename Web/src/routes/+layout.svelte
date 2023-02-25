<script lang="ts">
    import '../app.css';
    import {onMount} from 'svelte';

    onMount(async () => {
		// attempt to warm the lambda by calling /warm (/ping is reserved by API Gateway)
		fetch('https://api.cantilevers.org/warm', {
			mode: 'no-cors',
			headers: {
				Accept: 'text/plain'
			}
		});
	});
</script>

<slot />
