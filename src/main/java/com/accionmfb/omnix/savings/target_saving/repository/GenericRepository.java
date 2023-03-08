package com.accionmfb.omnix.savings.target_saving.repository;

import com.accionmfb.omnix.savings.target_saving.model.AppUser;

public interface GenericRepository
{

    AppUser getAppUserUsingUsername(String username);
}
