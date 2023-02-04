<script lang="ts">
	import { PUBLIC_COGNITO_CALLBACK_URL } from '$env/static/public';
	import jwt_decode from 'jwt-decode';
	import type { JwtPayload } from 'jwt-decode';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import { User } from '../models/authUser';
	import { userStore } from '../stores/userStore.svelte';

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
	console.log('Logging in via AWS Cognito to ' + loginUrl);

	onMount(async () => {
		console.log('ON MOUNT parseIdToken()');
		authToken = extractIdToken();
		console.log('ON MOUNT: authToken = ' + authToken);
		if (authToken) {
			tokenPayload = jwt_decode<JwtPayload>(authToken);
			console.log('ON MOUNT: jwt_decode(authToken) = ');
			console.dir(tokenPayload);
			if (tokenPayload) {
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
		console.log('AuthToken: ' + authToken);
		if (authToken) {
			console.log('Authenticated');
			console.log(authToken);
			return;
		}
		console.log('Not logged in, redirecting...');
		window.location.assign(loginUrl);
	}

	function extractIdToken() {
		console.log('Parsing code token ' + window.location);
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
	<button class="btn btn-sm btn-primary" title="Login" on:click={login}> Login </button>
{:else}
	<span class="pr-2">{$userStore.name}</span>
	<a
		href="{cognitoDomain}/logout?client_id={appClientId}&logout_uri={PUBLIC_COGNITO_CALLBACK_URL}"
		title="Logout"
	>
		<svg
			version="1.1"
			id="Capa_1"
			xmlns="http://www.w3.org/2000/svg"
			xmlns:xlink="http://www.w3.org/1999/xlink"
			viewBox="0 0 304.588 304.588"
			xml:space="preserve"
			height="32px"
			width="24px"
			class="svg-icon"
		>
			<g>
				<g>
					<g>
						<polygon
							class="svg-icon polygon"
							points="134.921,34.204 134.921,54.399 284.398,54.399 284.398,250.183 134.921,250.183 
				134.921,270.384 304.588,270.384 304.588,34.204 			"
						/>
					</g>
					<g>
						<polygon
							class="svg-icon polygon"
							points="150.27,223.581 166.615,239.931 254.26,152.286 166.615,64.651 150.27,80.979 
				210.013,140.733 0,140.733 0,163.838 210.008,163.838 			"
						/>
					</g>
				</g>
			</g>
		</svg>
	</a>
{/if}

<style>
	.svg-icon {
		height: 32px;
	}

	.svg-icon path,
	.svg-icon polygon,
	.svg-icon rect {
		fill: rgb(229, 231, 235);
	}

	.svg-icon circle {
		stroke: rgb(229, 231, 235);
		stroke-width: 1;
	}
</style>
