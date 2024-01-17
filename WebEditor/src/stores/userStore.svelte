<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import type { User } from '../models/user.svelte';

	export const userStore = writable<User | undefined>(undefined);

	
</script>
