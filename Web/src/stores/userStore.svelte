<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { User } from '../models/authUser';

	export const userStore = writable<User>();
</script>
