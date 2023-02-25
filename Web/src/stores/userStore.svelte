<script context="module" lang="ts">
    import {writable} from 'svelte/store';
    import type {User} from '../models/authUser';

    export const userStore = writable<User>();
	export const CLEAR_USER = { name: '', sub: '', email: '', token: '', auth_time: 0 };
</script>
