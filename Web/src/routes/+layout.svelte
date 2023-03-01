<script lang="ts">
    import '../app.css';
    import {onMount} from 'svelte';

    export const warmTimer = 60 * 1000;

	onMount(async () => {
		async function warm() {
			// attempt to warm the lambda by calling /warm (/ping is reserved by API Gateway)
			console.log('Keeping lambda warm...');
			fetch('https://api.cantilevers.org/warm', {
				mode: 'no-cors',
				headers: {
					Accept: 'text/plain'
				}
			});
		}

		const interval = setInterval(warm, warmTimer);
		warm();
		return () => clearInterval(interval);
	});
</script>

<slot />
