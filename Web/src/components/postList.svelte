<script lang="ts">
    import type {Post, Structure} from '../models/structure';
    import {onDestroy, onMount} from 'svelte';
    import {writable} from 'svelte/store';

    const structureStore = writable<Structure>({ layouts: [], posts: [], postCount: 0 });
    const postStore = writable<Post[]>();

    $: postsSorted = $postStore.sort((a, b) => new Date(b.lastUpdated).valueOf() - new Date(a.lastUpdated).valueOf());

    onMount(async () => {
        fetch('https://h2ezadb0cl.execute-api.eu-west-2.amazonaws.com/prod/structure', {
            method: 'GET',
            headers: {
                Accept: 'application/json'
            },
            mode: 'cors'
        })
            .then((response) => response.json())
            .then((data) => {
                console.log(data);
                structureStore.set(data.data);
            })
            .catch((error) => {
                console.log(error);
                return {};
            });
    });
    // structureStore.subscribe((value) => value);
    const structStoreUnsubscribe = structureStore.subscribe((data) => {
        postStore.set(data.posts);
    });

    onDestroy(structStoreUnsubscribe);

    // structureStore.subscribe((data) => {
    // 	posts = data.items[0].posts;
    // });
</script>

{#if $structureStore}
    <p>{$structureStore.postCount} posts</p>
{/if}
<ol class="list-decimal pl-10">
    {#each postsSorted as post}
        {@const postDateString = new Date(post.date).toLocaleDateString('en-GB')}
        <li>
            <a href="https://www.cantilevers.org/{post.url}" rel="noreferrer" target="_blank"
            >{post.title}</a
            > <span class="text-green-800">{postDateString}</span>
        </li>
    {:else}
        <li class="p-0">loading posts...</li>
    {/each}
</ol>
