package org.ownprofile.boundary.owner.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ownprofile.boundary.ProfileDTO;
import org.ownprofile.boundary.ProfileDtoOutCompareUtil;
import org.ownprofile.boundary.ProfileCreateAndUpdateDTO;
import org.ownprofile.boundary.ServiceIntegrationTestSession;
import org.ownprofile.boundary.owner.client.TestOwnerClient;
import org.ownprofile.profile.entity.IdInitializer;
import org.ownprofile.profile.entity.ProfileEntity;
import org.ownprofile.profile.entity.MyProfileRepositoryMock;
import org.ownprofile.profile.entity.ProfileSource;
import org.ownprofile.profile.entity.RepoProxies;
import org.ownprofile.profile.entity.TestProfileEntity;

// each testmethod invokes at most one method on the resource
public class MyProfilesResourceIT {

	private static final RepoProxies repoProxies = new RepoProxies();
	private static ServiceIntegrationTestSession session;

	private TestOwnerClient client;
	private MyProfileRepositoryMock profileRepoMock;

	@BeforeClass
	public static void startJetty() throws Exception {
		session = new ServiceIntegrationTestSession(repoProxies.createCustomModule());
		session.server.start();
	}

	@AfterClass
	public static void stopJetty() throws Exception {
		session.server.stop();
	}

	// =============================================================

	@Before
	public void setup() throws URISyntaxException {
		this.client = session.getOrCreateOwnerClient();
		
		final IdInitializer<ProfileEntity> profileIdInitializer = new IdInitializer<>(ProfileEntity.class);
		this.profileRepoMock = new MyProfileRepositoryMock(profileIdInitializer);
		repoProxies.setProfileRepository(profileRepoMock);
		
		this.profileRepoMock.addMyProfile(createMyProfile());
	}

	@After
	public void tearDown() {
		repoProxies.clearDelegates();
	}
	
	private ProfileEntity createMyProfile() {
		return TestProfileEntity.createOwnProfile(this.profileRepoMock.profileIdSource().nextId(), ProfileSource.createLocalSource(), "private");
		// new TestProfileEntity(92L, ProfileSource.createRemoteSource("http://localhost"), "professional");
	}

	// -------------------------------------------------------------

	@Test
	public void shouldGetMyProfiles() {
		final List<ProfileDTO> profiles = client.getMyProfiles();

		Assert.assertEquals(profileRepoMock.getMyProfiles().size(), profiles.size());

		final Iterator<ProfileEntity> expectedIt = profileRepoMock.getMyProfiles().iterator();
		final Iterator<ProfileDTO> actualIt = profiles.iterator();
		while (expectedIt.hasNext()) {
			ProfileDtoOutCompareUtil.assertMyProfileContentIsEqualOnOwnerAPI(expectedIt.next(), actualIt.next(), client.getUriBuilder());
		}
	}
	
	@Test
	public void shouldGetMyProfileById() {
		final Long profileId = profileRepoMock.addedProfile.getId().get();
		final ProfileDTO profile = client.getMyProfileById(profileId);

		Assert.assertNotNull(profile);

		final ProfileEntity expected = profileRepoMock.getMyProfileById(profileId).get();				
		ProfileDtoOutCompareUtil.assertMyProfileContentIsEqualOnOwnerAPI(expected, profile, client.getUriBuilder());
	}

	@Test
	public void shouldAddNewMyProfile() {
		final Map<String, Object> body = Collections.emptyMap();
		final ProfileCreateAndUpdateDTO newProfile = new ProfileCreateAndUpdateDTO("public", body);

		final URI location = client.addNewMyProfile(newProfile);

		final ProfileEntity actual = profileRepoMock.addedProfile;
		Assert.assertNotNull(actual);
		Assert.assertEquals(newProfile.profileName, actual.getProfileName());
		
		final URI expectedLocation = client.getUriBuilder().resolveMyProfileURI(actual.getId().get());
		Assert.assertEquals(expectedLocation, location);
	}

}
