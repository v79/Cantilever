<script>
    import PostList from '../components/postList.svelte';
</script>

<h1 class="text-3xl font-bold underline">Welcome to Cantilever Svelte Test</h1>

<div class="grid grid-cols-3">
    <div><PostList /></div>
    <div>Col 2</div>
    <div>Col 3</div>
</div>

<style lang="postcss">
    :global(html) {
        background-color: theme(colors.gray.200);
    }
</style>
