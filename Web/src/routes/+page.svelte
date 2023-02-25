<script lang="ts">
    import PostList from '../components/postList.svelte';
    import MarkdownEditor from '../components/markdownEditor.svelte';
    import {Footer, FooterCopyright, FooterLink, FooterLinkGroup} from 'flowbite-svelte';
    import Navigation from '../components/navigation.svelte';
    import SpinnerWrapper from '../components/utilities/spinnerWrapper.svelte';
</script>

<div class="flex h-screen flex-col">
	<Navigation />

	<div class="flex grow flex-row">
		<div class="basis-1/4 bg-slate-400">
			<PostList />
		</div>
		<div class="basis-1/2 bg-slate-600">
			<MarkdownEditor />
		</div>
		<div class="invisible basis-1/4 bg-slate-800 lg:visible">
			<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Messages</h3>
			<SpinnerWrapper spinnerID="globalSpinner" />
		</div>
	</div>

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

<style lang="postcss">
	:global(html) {
		background-color: theme(colors.gray.200);
	}
</style>
