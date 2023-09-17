<script lang="ts">
	import { PUBLIC_COGNITO_CALLBACK_URL } from '$env/static/public';
	import type { JwtPayload } from 'jwt-decode';
	import jwt_decode from 'jwt-decode';
	import { onMount } from 'svelte';
	import { User } from '../models/authUser';
	import { CLEAR_USER, userStore } from '../stores/userStore.svelte';

	let authToken: any;
	let tokenPayload: JwtPayload;

	const appClientId = '6ijb6bg3hk22selq6rj2bb5rmq';
	const cognitoDomain = 'https://cantilever.auth.eu-west-2.amazoncognito.com';
	const loginUrl =
		cognitoDomain +
		'/oauth2/authorize?response_type=token&client_id=' +
		appClientId +
		'&redirect_uri=' +
		PUBLIC_COGNITO_CALLBACK_URL +
		'&scope=openid+profile';

	const logoutUrl =
		cognitoDomain +
		'/logout?client_id=' +
		appClientId +
		'&logout_uri=' +
		PUBLIC_COGNITO_CALLBACK_URL;

	onMount(async () => {
		authToken = extractIdToken();
		if (authToken) {
			tokenPayload = jwt_decode<JwtPayload>(authToken);
			if (tokenPayload) {
				// TODO: quite a few typescript errors here
				$userStore = new User(
					tokenPayload.name,
					tokenPayload.sub,
					tokenPayload.email,
					tokenPayload.auth_time,
					authToken
				);
			}
		}
	});

	function login() {
		if (authToken) {
			return;
		}
		window.location.assign(loginUrl);
	}

	function logout() {
		userStore.set(CLEAR_USER);
		window.location.assign(logoutUrl);
	}

	function extractIdToken() {
		if (window.location.hash) {
			let hash = window.location.hash.substr(1);
			let regex = /id_token=([^&]*)/;
			let match = hash.match(regex);
			if (match) {
				return match[1];
			}
		}
		return null;
	}
</script>

{#if $userStore === undefined}
	<button type="button" class="btn-sm btn-primary btn" title="Login" on:click={login}>
		Login
	</button>
{:else}
	<img
		src="https://mdbcdn.b-cdn.net/img/new/avatars/2.webp"
		class="w-8 rounded-full"
		alt="Avatar" />
	<span class="pr-2">{$userStore.name}</span>
	<button type="button" title="Logout" on:click={logout}>
		<svg
			version="1.1"
			id="Capa_1"
			xmlns="http://www.w3.org/2000/svg"
			xmlns:xlink="http://www.w3.org/1999/xlink"
			viewBox="0 0 304.588 304.588"
			xml:space="preserve"
			height="32px"
			width="24px"
			class="svg-icon">
			<g>
				<g>
					<g>
						<polygon
							class="svg-icon polygon"
							points="134.921,34.204 134.921,54.399 284.398,54.399 284.398,250.183 134.921,250.183 
				134.921,270.384 304.588,270.384 304.588,34.204 			" />
					</g>
					<g>
						<polygon
							class="svg-icon polygon"
							points="150.27,223.581 166.615,239.931 254.26,152.286 166.615,64.651 150.27,80.979 
				210.013,140.733 0,140.733 0,163.838 210.008,163.838 			" />
					</g>
				</g>
			</g>
		</svg>
	</button>
{/if}

<style>
	.svg-icon {
		height: 32px;
	}

	.svg-icon polygon {
		fill: rgb(229, 231, 235);
	}
</style>
