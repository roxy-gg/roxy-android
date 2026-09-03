package gg.roxy.remote

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    @Binds
    @Singleton
    abstract fun bindRemoteStorage(
        impl: SharedPreferencesRemoteStorage,
    ): RemoteStorage

    @Binds
    @Singleton
    abstract fun bindRemoteWorkspaceClient(
        impl: DefaultRemoteWorkspaceClient,
    ): RemoteWorkspaceClient
}
