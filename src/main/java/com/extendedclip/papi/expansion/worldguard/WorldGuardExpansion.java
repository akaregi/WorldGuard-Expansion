/*
 *
 * WorldGuard-Expansion Copyright (C) 2018 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.worldguard;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team.Option;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class WorldGuardExpansion extends PlaceholderExpansion {
    @Getter(onMethod = @__({@Override}))
    private final String author = "clip";

    @Getter(onMethod = @__({@Override}))
    private final String name = "WorldGuard";

    @Getter(onMethod = @__({@Override}))
    private final String identifier = name.toLowerCase();

    @Getter(onMethod = @__({@Override}))
    private final String version = getClass().getPackage().getImplementationVersion();

    private WorldGuard worldguard;

    @Override
    public boolean canRegister() {
        val wgPlugin = Bukkit.getServer().getPluginManager().getPlugin(name);

        if (Objects.isNull(wgPlugin)) {
            return false;
        }

        val instance = Optional.ofNullable(WorldGuard.getInstance());

        worldguard = instance.orElseGet(null);

        return instance.isPresent();
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
        val player = (Player) offlinePlayer;

        if (Objects.isNull(player)) {
            return "";
        }

        if (!player.isOnline()) {
            return "";
        }

        val region = getRegion(player.getLocation());

        if (!region.isPresent()) {
            return "";
        }

        return processIdentifier(identifier, region.get())
                .orElseThrow(() -> new IllegalArgumentException("No such placeholder."));
    }

    /**
     * Returns the value corresponding to an identifier.
     *
     * @param identifier Identifier, like {@code region_name}, {@code region_members}, etc.
     * @param region     WG's region, used for processing
     *
     * @return Result as {@code Optional}, therefore maybe {@code null}.
     */
    private Optional<String> processIdentifier(@NonNull String identifier, @NonNull ProtectedRegion region) {
        String result;

        switch (identifier) {
            case "region_name":
                result = region.getId();
                break;

            case "region_owners":
                val owners =region.getOwners().getPlayerDomain().getUniqueIds().stream()
                    .map(uuid -> {
                        return Bukkit.getOfflinePlayer(uuid).getName();
                    }).collect(Collectors.toList());

                result = owners.isEmpty() ? "" : String.join(", ", owners);
                break;

            case "region_members":
                val members = region.getMembers().getPlayerDomain().getUniqueIds().stream()
                    .map(uuid -> {
                        return Bukkit.getOfflinePlayer(uuid).getName();
                    }).collect(Collectors.toList());

                result = members.isEmpty() ? "" : String.join(", ", members);
                break;

            case "region_flags":
                result = region.getFlags().entrySet().toString();
                break;

            default:
                return Optional.empty();
        }

        return Optional.ofNullable(result);
    }

    /**
     * Gets a region from the location.
     *
     * @param location Location
     *
     * @return a region, maybe {@code null}.
     */
    private Optional<ProtectedRegion> getRegion(@NonNull Location location) {
        val manager = worldguard
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(location.getWorld()));

        if (Objects.isNull(manager)) {
            return Optional.empty();
        }

        try {
            val vector  = BukkitAdapter.adapt(location).toVector().toBlockPoint();
            val regions = manager.getApplicableRegionsIDs(vector);

            return Optional.ofNullable(manager.getRegion(regions.get(0)));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}
