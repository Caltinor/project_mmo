package harmonised.pmmo.events;

import harmonised.pmmo.config.FConfig;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.config.JsonConfig;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import harmonised.pmmo.util.DP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DeathHandler
{
    public static void handleDeath( LivingDeathEvent event )
    {
        EntityLivingBase target = event.getEntityLiving();
        Entity source = event.getSource().getTrueSource();
        double deathPenaltyMultiplier = FConfig.deathPenaltyMultiplier;
        double passiveMobHunterXp = FConfig.passiveMobHunterXp;
        double aggresiveMobSlayerXp = FConfig.aggresiveMobSlayerXp;
        boolean deathLoosesLevels = FConfig.deathLoosesLevels;

        if( target instanceof EntityPlayerMP && !( target instanceof FakePlayer ) )
        {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
            if( !player.world.isRemote )
            {
                Map<String, Double> xpMap = FConfig.getXpMap( player );
                Map<String, Double> prefsMap = FConfig.getPreferencesMap( player );
                double totalLost = 0;
                boolean wipeAllSkills = FConfig.wipeAllSkillsUponDeathPermanently;
                if( prefsMap.containsKey( "wipeAllSkillsUponDeathPermanently" ) && prefsMap.get( "wipeAllSkillsUponDeathPermanently" ) != 0 )
                    wipeAllSkills = true;

                if( wipeAllSkills )
                {
                    for( Map.Entry<String, Double> entry : new HashMap<>( xpMap ).entrySet() )
                    {
                        totalLost += entry.getValue();
                        xpMap.remove( entry.getKey() );
                    }
                }
                else
                {
                    for( Map.Entry<String, Double> entry : new HashMap<>( xpMap ).entrySet() )
                    {
                        double startXp = entry.getValue();
                        double floorXp = XP.xpAtLevelDecimal( Math.floor( XP.levelAtXpDecimal( startXp ) ) );
                        double diffXp = startXp - floorXp;
                        double lostXp;
                        if( deathLoosesLevels )
                        {
                            double requiredLevel = XP.levelAtXpDecimal( startXp ) * deathPenaltyMultiplier;
                            lostXp = startXp - XP.xpAtLevelDecimal( requiredLevel );
                        }
                        else
                            lostXp = diffXp * deathPenaltyMultiplier;
                        double finalXp = startXp - lostXp;
                        totalLost += lostXp;

                        if( finalXp > 0 )
                            xpMap.put( entry.getKey(), finalXp );
                        else
                            xpMap.remove( entry.getKey() );
                    }
                }

                if( totalLost > 0 )
                    player.sendStatusMessage( new TextComponentTranslation( "pmmo.lostXp", DP.dprefix( totalLost ) ).setStyle( XP.textStyle.get( "red" ) ), false );

                XP.syncPlayer( player );
            }
        }
        else if( source instanceof EntityPlayerMP && !( source instanceof FakePlayer ) )
        {
            EntityPlayerMP player = (EntityPlayerMP) source;
            Collection<EntityPlayer> nearbyPlayers = XP.getNearbyPlayers( target );
            double scaleValue = 0;

            for( EntityPlayer thePlayer : nearbyPlayers )
            {
                if( XP.getPowerLevel( player.getUniqueID() ) > 1 )
                    scaleValue += 1;
                else
                    scaleValue += XP.getPowerLevel( thePlayer.getUniqueID() );
            }

            scaleValue /= 5;

            if( scaleValue < 1 )
                scaleValue = 1;

            if( scaleValue > 10 )
                scaleValue = 10;

//            double normalMaxHp = target.getAttributeMap().getAttribute( Attributes.GENERIC_MAX_HEALTH ).getBaseValue();
//            double scaleMultiplier = ( 1 + ( target.getMaxHealth() - normalMaxHp ) / 10 );

            if( JsonConfig.data.get( JType.XP_VALUE_KILL ).containsKey( target.getName() ) )
            {
                Map<String, Double> killXp = JsonConfig.data.get( JType.XP_VALUE_KILL ).get( target.getName() );
                for( Map.Entry<String, Double> entry : killXp.entrySet() )
                {
                    XP.awardXp( player, entry.getKey(), player.getHeldItemMainhand().getDisplayName().toString(), entry.getValue() * scaleValue, false, false, false );
                }
            }
            else if( target instanceof EntityAnimal)
                XP.awardXp( player, Skill.HUNTER.toString(), player.getHeldItemMainhand().getDisplayName().toString(), passiveMobHunterXp * scaleValue, false, false, false );
            else if( target instanceof EntityMob )
                XP.awardXp( player, Skill.SLAYER.toString(), player.getHeldItemMainhand().getDisplayName().toString(), aggresiveMobSlayerXp * scaleValue, false, false, false );

            if( JsonConfig.data.get( JType.MOB_RARE_DROP ).containsKey( target.getName() ) )
            {
                Map<String, Double> dropTable = JsonConfig.data.get( JType.MOB_RARE_DROP ).get( target.getName() );

                double chance;

                for( Map.Entry<String, Double> entry : dropTable.entrySet() )
                {
                    chance = entry.getValue();
                    chance /= scaleValue;

                    if( Math.floor( Math.random() * chance ) == 0 )
                    {
                        ItemStack itemStack = new ItemStack( XP.getItem( entry.getKey() ) );
                        XP.dropItemStack( itemStack, player.world, target.getPositionVector() );

                        player.sendStatusMessage( new TextComponentTranslation( "pmmo.rareDrop", new TextComponentTranslation( itemStack.getDisplayName() ) ).setStyle( XP.textStyle.get( "green" ) ), false );
                        player.sendStatusMessage( new TextComponentTranslation( "pmmo.rareDrop", new TextComponentTranslation( itemStack.getDisplayName() ) ).setStyle( XP.textStyle.get( "green" ) ), true );
                    }
                }
            }
        }
    }

    public static boolean canBeSalvaged( Item item )
    {
        return JsonConfig.data2.get( JType.SALVAGE ).containsKey( item.getRegistryName().toString() );
    }
}