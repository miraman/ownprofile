package org.ownprofile.profile.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProfileRepositoryMock implements ProfileRepository {
	public final IdSource profileIdSource = new IdSource();

	private final Map<Long, ProfileEntity> ownerProfiles = new HashMap<Long, ProfileEntity>();
	private final Map<ProfileHandle, ProfileEntity> ownerProfilesByHandle = new HashMap<ProfileHandle, ProfileEntity>();
	
	public ProfileEntity addedProfile;

	private final Field profileIdField;

	public ProfileRepositoryMock() {
		try {
			this.profileIdField = ProfileEntity.class.getDeclaredField("id");
			this.profileIdField.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public List<ProfileEntity> getAllOwnerProfiles() {
		return new ArrayList<ProfileEntity>(ownerProfiles.values());
	}

	@Override
	public Optional<ProfileEntity> getOwnerProfileById(long id) {
		return Optional.ofNullable(ownerProfiles.get(id));
	}
	
	@Override
	public Optional<ProfileEntity> getOwnerProfileByHandle(ProfileHandle handle) {
		return Optional.ofNullable(ownerProfilesByHandle.get(handle));
	}

	@Override
	public void addProfile(ProfileEntity profile) {
		final Long id = initializeIdIfNull(profile);
		if (ownerProfiles.containsKey(id)) {
			throw new IllegalStateException(String.format("Repo already contains ProfileEntity with id[%d]", id));
		}
		
		final ProfileHandle handle = profile.getHandle().get();
		if (ownerProfilesByHandle.containsKey(handle)) {
			throw new IllegalStateException(String.format("Repo already contains ProfileEntity with handle[%s]", handle));
		}

		this.ownerProfiles.put(id, profile);
		this.ownerProfilesByHandle.put(handle, profile);
		this.addedProfile = profile;
	}

	private Long initializeIdIfNull(ProfileEntity profile) {
		try {
			Long id = (Long) profileIdField.get(profile);
			if (id == null) {
				id = this.profileIdSource.nextId();
				this.profileIdField.set(profile, id);
			}
			return id;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

	}

}