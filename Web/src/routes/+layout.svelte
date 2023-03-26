<script lang="ts">
    import '../app.css';
    import {onMount} from 'svelte';
    import {Footer, FooterCopyright, FooterLink, FooterLinkGroup} from 'flowbite-svelte';
    import Navigation from '../components/navigation.svelte';

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

<div class="flex h-screen flex-col">
	<Navigation />

	<slot />

	<Footer>
		<div class="grid md:grid-cols-2 lg:grid-cols-4">
			<div class="mb-6">
				<h5 class="mb-2.5 font-bold uppercase text-gray-800">About Cantilever</h5>
				<FooterLinkGroup>
					<FooterLink href="https://www.cantilevers.org/">Project History</FooterLink>
				</FooterLinkGroup>
			</div>
		</div>
		<FooterCopyright href="https://www.cantilevers.org/app/" by="Liam Davison" year={2023} />
	</Footer>
</div>
