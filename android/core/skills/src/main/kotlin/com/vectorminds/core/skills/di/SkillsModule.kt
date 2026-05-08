package com.vectorminds.core.skills.di

import com.vectorminds.core.network.VectorMindsApi
import com.vectorminds.core.skills.AlertSkill
import com.vectorminds.core.skills.AuthorContextSkill
import com.vectorminds.core.skills.BlueprintGeneratorSkill
import com.vectorminds.core.skills.PipelineLauncherSkill
import com.vectorminds.core.skills.Skill
import com.vectorminds.core.skills.TrendMonitorSkill
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object SkillsModule {

    @Provides
    @IntoSet
    fun provideTrendMonitorSkill(api: VectorMindsApi): Skill =
        TrendMonitorSkill(api)

    @Provides
    @IntoSet
    fun provideBlueprintGeneratorSkill(api: VectorMindsApi): Skill =
        BlueprintGeneratorSkill(api)

    @Provides
    @IntoSet
    fun providePipelineLauncherSkill(api: VectorMindsApi): Skill =
        PipelineLauncherSkill(api)

    @Provides
    @IntoSet
    fun provideAlertSkill(api: VectorMindsApi): Skill =
        AlertSkill(api)

    @Provides
    @IntoSet
    fun provideAuthorContextSkill(): Skill =
        AuthorContextSkill()
}
